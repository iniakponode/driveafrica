package com.uoa.sensor.services

import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleService
import androidx.core.content.ContextCompat
import com.uoa.core.apiServices.models.tripModels.TripCreate
import com.uoa.core.apiServices.services.tripApiService.TripApiRepository
import com.uoa.core.database.daos.AlcoholQuestionnaireResponseDao
import com.uoa.core.database.daos.DriverProfileDAO
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.mlclassifier.data.InferenceResult
import com.uoa.core.model.Trip
import com.uoa.core.notifications.VehicleNotificationManager
import com.uoa.core.utils.DateUtils
import com.uoa.core.utils.PreferenceUtils
import com.uoa.core.utils.Resource
import com.uoa.ml.domain.RunClassificationUseCase
import com.uoa.sensor.domain.usecases.trip.GetTripByIdUseCase
import com.uoa.sensor.domain.usecases.trip.InsertTripUseCase
import com.uoa.sensor.domain.usecases.trip.UpdateTripUseCase
import com.uoa.sensor.hardware.HardwareModule
import com.uoa.sensor.location.LocationManager
import com.uoa.sensor.motion.DrivingStateManager
import com.uoa.sensor.repository.SensorDataColStateRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Foreground Service that monitors vehicle movement and automatically
 * starts/stops trip data collection based on sustained motion detection.
 */
@AndroidEntryPoint
open class VehicleMovementServiceUpdate : LifecycleService() {
    @RequiresApi(Build.VERSION_CODES.S)
    private fun isForegroundStartAllowedException(throwable: Throwable): Boolean {
        return throwable is android.app.ForegroundServiceStartNotAllowedException
    }

    @Inject lateinit var hardwareModule: HardwareModule
    @Inject lateinit var sensorRepo: SensorDataColStateRepository

    @Inject lateinit var insertTripUseCase: InsertTripUseCase
    @Inject lateinit var updateTripUseCase: UpdateTripUseCase
    @Inject lateinit var runClassificationUseCase: RunClassificationUseCase
    @Inject lateinit var getTripByIdUseCase: GetTripByIdUseCase
    @Inject lateinit var tripApiRepository: TripApiRepository
    @Inject lateinit var localTripRepository: TripDataRepository
    @Inject lateinit var locationManager: LocationManager
    @Inject lateinit var questionnaireDao: AlcoholQuestionnaireResponseDao
    @Inject lateinit var driverProfileDao: DriverProfileDAO

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentTripId: UUID? = null
    private var driverProfileId: UUID? = null
    private var stateJob: Job? = null
    private var fallbackSpeedJob: Job? = null
    private var isInitialized = false
    private val startStopMutex = Mutex()

    private lateinit var notificationManager: VehicleNotificationManager

    companion object {
        const val TAG = "VehicleMovementSvc"
//        private const val CHANNEL_ID = "vehicle_movement"


    }

    override fun onCreate() {
        super.onCreate()
//        createNotificationChannel()
        notificationManager = VehicleNotificationManager(this)
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val appContext = application.applicationContext

        if (!hasLocationPermission()) {
            Log.w(TAG, "Missing location permission; stopping movement service.")
            notificationManager.displayPermissionNotification(
                "Grant location permission to start monitoring"
            )
            stopSelf()
            return START_NOT_STICKY
        }

        reconcileTripState()

        driverProfileId = PreferenceUtils.getDriverProfileId(appContext)
        if (driverProfileId != null) {
            Log.d(TAG, "Profile ID found: $driverProfileId")
        } else {
            Log.d(TAG, "No profile ID found in persistent storage")
            stopSelf()
            return START_NOT_STICKY
        }
        val localProfile = runBlocking(Dispatchers.IO) {
            driverProfileDao.getDriverProfileById(driverProfileId!!)
        }
        if (localProfile == null) {
            Log.w(TAG, "Driver profile missing locally; blocking monitoring until hydrated.")
            notificationManager.displayPermissionNotification(
                "Open the app to finish profile setup before monitoring can start."
            )
            stopSelf()
            return START_NOT_STICKY
        }
        currentTripId = sensorRepo.currentTripId.value

        // Start as foreground service
        try {
            startForeground(
                VehicleNotificationManager.FOREGROUND_NOTIFICATION_ID,
                notificationManager.buildForegroundNotification(
                    title = "Safe Drive Africa",
                    message = "Monitoring for trips (location in use)"
                )
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to start foreground service; stopping", e)
            if (!hasLocationPermission()) {
                notificationManager.displayPermissionNotification(
                    "Open the app and grant location permission to start monitoring"
                )
            } else {
                Log.w(TAG, "Foreground start failed even though location permission is granted.")
            }
            stopSelf()
            return START_NOT_STICKY
        } catch (e: RuntimeException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isForegroundStartAllowedException(e)) {
                Log.e(TAG, "Foreground start not allowed; stopping", e)
                notificationManager.displayPermissionNotification(
                    "Open the app to start monitoring"
                )
                stopSelf()
                return START_NOT_STICKY
            }
            throw e
        }

        // Begin motion detection
        if (!isInitialized) {
            connectDrivingStateManager()
            isInitialized = true
        }

        return START_STICKY
    }

    private fun reconcileTripState() {
        if (isDataCollectionServiceRunning()) return
        val hasStaleTrip = sensorRepo.tripStartStatus.value ||
            sensorRepo.collectionStatus.value ||
            sensorRepo.currentTripId.value != null
        if (!hasStaleTrip) return
        Log.w(TAG, "DataCollectionService not running; clearing stale trip state")
        currentTripId = null
        serviceScope.launch {
            sensorRepo.updateCollectionStatus(false)
            sensorRepo.startTripStatus(false)
            sensorRepo.updateCurrentTripId(null)
            sensorRepo.updateDrivingState(DrivingStateManager.DrivingState.IDLE)
        }
    }

    @Suppress("DEPRECATION")
    private fun isDataCollectionServiceRunning(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as? ActivityManager
            ?: return false
        val services = activityManager.getRunningServices(Int.MAX_VALUE)
        return services.any { it.service.className == DataCollectionService::class.java.name }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        hardwareModule.getDrivingStateManager().stopMonitoring()
        hardwareModule.stop()
        stateJob?.cancel()
        fallbackSpeedJob?.cancel()
        isInitialized = false
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        // We’re not supporting binding, so just return null.
        return null
    }

    private fun connectDrivingStateManager() {
        val drivingStateManager = hardwareModule.getDrivingStateManager()

        locationManager.setLocationCallback { location ->
            drivingStateManager.updateLocation(location)
        }

        drivingStateManager.initialize(object : DrivingStateManager.StateCallback {
            override fun onDriveStarted() {
                serviceScope.launch { safeAutoStart() }
            }

            override fun onDriveStopped() {
                serviceScope.launch { safeAutoStop() }
            }

            override fun requestGpsEnable() {
                locationManager.startLocationUpdates()
                locationManager.setDrivingState(drivingStateManager.currentState.value)
            }

            override fun requestGpsDisable() {
                locationManager.stopLocationUpdates()
            }

            override fun onStateChanged(newState: DrivingStateManager.DrivingState) {
                serviceScope.launch {
                    locationManager.setDrivingState(newState)
                    sensorRepo.updateDrivingState(newState)
                    updateMovementNotification(newState)
                    val hasTrip = sensorRepo.tripStartStatus.value || currentTripId != null
                    if (hasTrip) {
                        when (newState) {
                            DrivingStateManager.DrivingState.POTENTIAL_STOP -> hardwareModule.pauseDataCollection()
                            DrivingStateManager.DrivingState.RECORDING -> hardwareModule.resumeDataCollection()
                            else -> Unit
                        }
                    } else {
                        locationManager.setRecordingEnabled(false)
                    }
                }
            }
        })

        drivingStateManager.startMonitoring()

        stateJob?.cancel()
        stateJob = serviceScope.launch {
            combine(
                drivingStateManager.currentVariance,
                drivingStateManager.currentSpeedMph,
                drivingStateManager.currentAccuracy
            ) { variance, speedMph, accuracy ->
                Triple(variance, speedMph, accuracy)
            }.collect { (variance, speedMph, accuracy) ->
                sensorRepo.updateDrivingMetrics(variance, speedMph / 2.23694, accuracy)
            }
        }

        fallbackSpeedJob?.cancel()
        fallbackSpeedJob = serviceScope.launch {
            combine(
                sensorRepo.fusedSpeedMps,
                sensorRepo.isGpsStale
            ) { speedMps, isGpsStale ->
                Pair(speedMps, isGpsStale)
            }.collect { (speedMps, isGpsStale) ->
                drivingStateManager.updateFallbackSpeed(speedMps, isGpsStale)
            }
        }
    }

    private fun updateMovementNotification(state: DrivingStateManager.DrivingState) {
        val message = when (state) {
            DrivingStateManager.DrivingState.IDLE -> "Monitoring for trips (location in use)"
            DrivingStateManager.DrivingState.VERIFYING -> "Checking for trip start (location in use)"
            DrivingStateManager.DrivingState.RECORDING -> "Trip active — collecting location and sensor data"
            DrivingStateManager.DrivingState.POTENTIAL_STOP -> "Trip ending — monitoring for stop"
        }
        val notification = notificationManager.buildForegroundNotification(
            title = "Safe Drive Africa",
            message = message
        )
        startForeground(VehicleNotificationManager.FOREGROUND_NOTIFICATION_ID, notification)
    }

    protected open suspend fun safeAutoStart() {
        startStopMutex.withLock {
            if (hasActiveTrip()) {
                Log.w(TAG, "Trip already active; ignoring auto-start request")
                return@withLock
            }
            val driverId = driverProfileId
            if (driverId == null) {
                Log.w(TAG, "Missing driver profile ID; cannot auto-start trip")
                return@withLock
            }
            val tripId = UUID.randomUUID().also { currentTripId = it }
            val startTime = System.currentTimeMillis()
            try {
                // Build trip
                val trip = Trip(
                    driverPId = driverId,
                    startTime = startTime,
                    endTime = null,
                    startDate = Date(startTime),
                    endDate = null,
                    id = tripId,
                    influence = "",
                    sync = false
                )
                insertTripUseCase(trip)

                // Update state flows
                sensorRepo.startTripStatus(true)
                sensorRepo.updateCollectionStatus(true)
                sensorRepo.updateCurrentTripId(tripId)

                // Launch data collection
                Intent(this@VehicleMovementServiceUpdate, DataCollectionService::class.java).apply {
                    putExtra("TRIP_ID", tripId.toString())
                }.also { intent ->
                    startForegroundService(intent)
                }

                Log.i(TAG, "Auto-started trip $tripId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to auto-start trip", e)
                currentTripId = null
                sensorRepo.startTripStatus(false)
                sensorRepo.updateCollectionStatus(false)
                sensorRepo.updateCurrentTripId(null)
                return@withLock
            }

            serviceScope.launch {
                val isoDate = utcIso(startTime)
                val payload = TripCreate(
                    id = tripId,
                    driverProfileId = driverId,
                    startDate = isoDate,
                    endDate = null,
                    startTime = startTime,
                    endTime = null,
                    timeZoneId = timeZoneId(),
                    timeZoneOffsetMinutes = timeZoneOffsetMinutes(startTime),
                    sync = true,
                    influence = ""
                )
                when (val createResult = tripApiRepository.createTrip(payload)) {
                    is Resource.Success -> {
                        localTripRepository.updateUploadStatus(tripId, true)
                    }
                    is Resource.Error -> {
                        Log.w(TAG, "Remote trip create failed; will sync later: ${createResult.message}")
                        notificationManager.displayUploadFailure(
                            "Trip start upload failed. Will retry."
                        )
                    }
                    Resource.Loading -> {
                        Log.d(TAG, "Remote trip create in progress for $tripId")
                    }
                }
            }
        }
    }

    protected open suspend fun safeAutoStop() {
        startStopMutex.withLock {
            val tripId = currentTripId ?: sensorRepo.currentTripId.value ?: return@withLock
            currentTripId = tripId
            try {
                stopService(Intent(this@VehicleMovementServiceUpdate, DataCollectionService::class.java))

                // Classification
                val inference = runClassificationUseCase.invoke(tripId)
                val label = when (inference) {
                    is InferenceResult.Success -> {
                        Log.i(TAG, "Inference probability=${inference.probability}")
                        if (inference.isAlcoholInfluenced) "alcohol" else "No influence"
                    }
                    else -> "Not enough data"
                }
                val localTrip = getTripByIdUseCase.invoke(tripId)
                    ?: run {
                        Log.e(TAG, "No local trip found for id: $tripId during auto-stop")
                        return@withLock
                    }
                val startDate = localTrip.startDate
                    ?: run {
                        Log.e(TAG, "Missing start date for trip $tripId during auto-stop")
                        return@withLock
                    }
                val probability = (inference as? InferenceResult.Success)?.probability
                val tripEndTime = System.currentTimeMillis()
                val userAlcoholResponse = resolveUserAlcoholResponse(
                    localTrip.driverPId,
                    localTrip.startDate,
                    tripEndTime
                )

                // Update local and remote
                updateTripUseCase(tripId, label, probability, userAlcoholResponse)
                val payload = TripCreate(
                    id = tripId,
                    driverProfileId = localTrip.driverPId,
                    startDate = utcIso(startDate.time),
                    endDate = utcIso(tripEndTime),
                    startTime = localTrip.startTime,
                    endTime = tripEndTime,
                    influence = label,
                    timeZoneId = timeZoneId(),
                    timeZoneOffsetMinutes = timeZoneOffsetMinutes(localTrip.startTime),
                    sync = true,
                    userAlcoholResponse = userAlcoholResponse,
                    alcoholProbability = probability
                )
                when (val updateResult = tripApiRepository.updateTrip(tripId, payload)) {
                    is Resource.Success -> {
                        localTripRepository.updateUploadStatus(tripId, true)
                    }
                    is Resource.Error -> {
                        Log.w(TAG, "Remote trip update failed; will sync later: ${updateResult.message}")
                        notificationManager.displayUploadFailure(
                            "Trip update upload failed. Will retry."
                        )
                    }
                    Resource.Loading -> {
                        Log.d(TAG, "Remote trip update in progress for $tripId")
                    }
                }

                Log.i(TAG, "Auto-stopped trip $tripId with label $label")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to auto-stop trip", e)
            } finally {
                currentTripId = null
                sensorRepo.startTripStatus(false)
                sensorRepo.updateCollectionStatus(false)
                sensorRepo.updateCurrentTripId(null)
            }
        }
    }

    private suspend fun resolveUserAlcoholResponse(
        driverId: UUID?,
        tripStartDate: Date?,
        tripEndTime: Long
    ): String {
        if (driverId == null) return "00"
        val tripDay = tripStartDate?.let { DateUtils.convertToLocalDate(it.time) }
            ?: DateUtils.convertToLocalDate(tripEndTime)
        val tripDate = DateUtils.convertLocalDateToDate(tripDay)
        val response = questionnaireDao.getQuestionnaireResponseForDate(driverId, tripDate)
        return when {
            response == null -> "00"
            response.drankAlcohol -> "1"
            else -> "0"
        }
    }

    private fun utcIso(time: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(time))

    private fun timeZoneId(): String = TimeZone.getDefault().id

    private fun timeZoneOffsetMinutes(epochMs: Long): Int =
        TimeZone.getDefault().getOffset(epochMs) / 60000

    private fun hasActiveTrip(): Boolean {
        return currentTripId != null ||
            sensorRepo.tripStartStatus.value ||
            sensorRepo.currentTripId.value != null
    }
}
