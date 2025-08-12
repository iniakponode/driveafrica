package com.uoa.sensor.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.uoa.core.apiServices.models.tripModels.TripCreate
import com.uoa.core.apiServices.services.tripApiService.TripApiRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.mlclassifier.data.InferenceResult
import com.uoa.core.model.Trip
import com.uoa.core.notifications.VehicleNotificationManager
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.ml.domain.RunClassificationUseCase
import com.uoa.sensor.domain.usecases.trip.GetTripByIdUseCase
import com.uoa.sensor.domain.usecases.trip.InsertTripUseCase
import com.uoa.sensor.domain.usecases.trip.UpdateTripUseCase
import com.uoa.sensor.hardware.HardwareModule
import com.uoa.sensor.repository.SensorDataColStateRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Foreground Service that monitors vehicle movement and automatically
 * starts/stops trip data collection based on sustained motion detection.
 */
@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.O)
class VehicleMovementServiceUpdate : LifecycleService() {
    @Inject lateinit var hardwareModule: HardwareModule
    @Inject lateinit var sensorRepo: SensorDataColStateRepository

    @Inject lateinit var insertTripUseCase: InsertTripUseCase
    @Inject lateinit var updateTripUseCase: UpdateTripUseCase
    @Inject lateinit var runClassificationUseCase: RunClassificationUseCase
    @Inject lateinit var getTripByIdUseCase: GetTripByIdUseCase
    @Inject lateinit var tripApiRepository: TripApiRepository
    @Inject lateinit var localTripRepository: TripDataRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentTripId: UUID? = null
    private var driverProfileId: UUID? = null
    private var startJob: Job? = null
    private var stopJob: Job? = null

    // Debounce windows
    internal var movementStartDelay = 10_000L
    internal var movementStopDelay = 30_000L

    private lateinit var notificationManager: VehicleNotificationManager

    companion object {
        private const val TAG = "VehicleMovementSvc"
        private const val NOTIFICATION_ID = 103
        private const val CHANNEL_ID = "vehicle_movement"


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

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Retrieve driverProfileId
        val profileIdString = prefs.getString(DRIVER_PROFILE_ID, null)
        if (profileIdString != null) {
            driverProfileId = UUID.fromString(profileIdString)
            Log.d("DrivingTipsViewModel", "Profile ID found: $driverProfileId")
        } else {
            Log.d("TAG", "No profile ID found in shared preferences")
            stopSelf()
            return START_NOT_STICKY
        }

        // Start as foreground service
        startForeground(
            NOTIFICATION_ID,
            notificationManager.buildForegroundNotification(
                title = "Vehicle Movement Service",
                message = "Monitoring vehicle movement continuously"
            )
        )

        // Begin motion detection
        hardwareModule.startMovementDetection()
        observeMovement()

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        hardwareModule.stopMovementDetection()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        // We’re not supporting binding, so just return null.
        return null
    }

    internal fun observeMovement() {
        serviceScope.launch {
            sensorRepo.isVehicleMoving
                .distinctUntilChanged()
                .collect { moving ->
                    if (moving) handlePossibleStart() else handlePossibleStop()
                }
        }
    }

    internal fun handlePossibleStart() {
        stopJob?.cancel()
        if (currentTripId == null) {
            startJob?.cancel()
            startJob = serviceScope.launch {
                delay(movementStartDelay)
                if (sensorRepo.isVehicleMoving.value) safeAutoStart()
            }
        }
    }

    internal fun handlePossibleStop() {
        startJob?.cancel()
        if (currentTripId != null) {
            stopJob?.cancel()
            stopJob = serviceScope.launch {
                delay(movementStopDelay)
                if (!sensorRepo.isVehicleMoving.value) safeAutoStop()
            }
        }
    }

    protected open suspend fun safeAutoStart() {
        val tripId = UUID.randomUUID().also { currentTripId = it }
        val startTime = System.currentTimeMillis()
        try {
            // Build trip
            val trip = Trip(
                driverPId = driverProfileId,
                startTime = startTime,
                endTime = null,
                startDate = Date(startTime),
                endDate = null,
                id = tripId,
                influence = "",
                sync = true
            )
            insertTripUseCase(trip)

            // Remote create
            val isoDate = utcIso(startTime)
            val payload = TripCreate(
                id = tripId,
                driverProfileId = driverProfileId,
                startDate = isoDate,
                endDate = null,
                startTime = startTime,
                endTime = null,
                sync = true,
                influence = ""
            )
            tripApiRepository.createTrip(payload)

            // Update state flows
            sensorRepo.startTripStatus(true)
            sensorRepo.updateCollectionStatus(true)

            // Launch data collection
            Intent(this@VehicleMovementServiceUpdate, DataCollectionService::class.java).apply {
                putExtra("TRIP_ID", tripId.toString())
            }.also { intent ->
                startForegroundService(intent)
            }


            Log.i(TAG, "Auto‐started trip $tripId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto‐start trip", e)
            currentTripId = null
        }
    }

    protected open suspend fun safeAutoStop() {
        val tripId = currentTripId ?: return
        try {
            stopService(Intent(this@VehicleMovementServiceUpdate, DataCollectionService::class.java))

            // Classification
            val inference = runClassificationUseCase.invoke(tripId)
            val label = when (inference) {
                is InferenceResult.Success -> if (inference.isAlcoholInfluenced) "alcohol" else "No influence"
                else -> "Not enough data"
            }

            // Update local and remote
            updateTripUseCase(tripId, label)
            sensorRepo.startTripStatus(false)
            sensorRepo.updateCollectionStatus(false)

            val localTrip = getTripByIdUseCase.invoke(tripId)
            val payload = TripCreate(
                id = tripId,
                driverProfileId = localTrip.driverPId,
                startDate = utcIso(localTrip.startDate!!.time),
                endDate = utcIso(System.currentTimeMillis()),
                startTime = localTrip.startTime,
                endTime = System.currentTimeMillis(),
                influence = label,
                sync = true
            )
            tripApiRepository.updateTrip(tripId, payload)
            localTripRepository.updateUploadStatus(tripId, true)

            Log.i(TAG, "Auto‐stopped trip $tripId with label $label")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto‐stop trip", e)
        } finally {
            currentTripId = null
        }
    }


    private fun utcIso(time: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC+1")
        }.format(Date(time))
}