package com.uoa.sensor.services

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import com.uoa.core.notifications.VehicleNotificationManager
import com.uoa.sensor.hardware.HardwareModule
import com.uoa.sensor.repository.SensorDataColStateRepository

@AndroidEntryPoint
class DataCollectionService : Service() {

    @Inject
    lateinit var hardwareModule: HardwareModule

    @Inject
    lateinit var notificationManager: VehicleNotificationManager

    @Inject
    lateinit var sensorRepo: SensorDataColStateRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var restoreJob: kotlinx.coroutines.Job? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val tripIdString = intent?.getStringExtra("TRIP_ID")
        val tripIdFromIntent = tripIdString?.let {
            runCatching { UUID.fromString(it) }
                .onFailure { e ->
                    Log.e(
                        "DataCollectionService",
                        "Invalid Trip ID provided: $it",
                        e
                    )
                }
                .getOrNull()
        }

        val tripId = tripIdFromIntent ?: sensorRepo.currentTripId.value

        if (tripId == null) {
            Log.w("DataCollectionService", "No trip id available yet; waiting for restore.")
            startForeground(
                VehicleNotificationManager.FOREGROUND_NOTIFICATION_ID,
                notificationManager.buildForegroundNotification(
                    title = "Restoring trip",
                    message = "Waiting for trip state to resume data collection"
                )
            )
            restoreJob?.cancel()
            restoreJob = serviceScope.launch {
                val restoredId = withTimeoutOrNull(15_000L) {
                    sensorRepo.currentTripId.filterNotNull().first()
                }
                if (restoredId != null) {
                    startCollection(restoredId)
                } else {
                    Log.e("DataCollectionService", "Trip restore timed out; stopping service.")
                    sensorRepo.updateCollectionStatus(false)
                    sensorRepo.startTripStatus(false)
                    sensorRepo.updateCurrentTripId(null)
                    stopSelf()
                }
            }
            return START_STICKY
        }

        restoreJob?.cancel()
        startCollection(tripId)

        return START_STICKY
    }

    private fun startCollection(tripId: UUID) {
        if (!hasLocationPermission()) {
            Log.w("DataCollectionService", "Missing location permission; stopping collection.")
            notificationManager.displayPermissionNotification(
                "Grant location permission to collect trip data"
            )
            serviceScope.launch {
                sensorRepo.updateCollectionStatus(false)
                sensorRepo.startTripStatus(false)
                sensorRepo.updateCurrentTripId(null)
            }
            stopSelf()
            return
        }

        try {
            startForeground(
                VehicleNotificationManager.FOREGROUND_NOTIFICATION_ID,
                notificationManager.buildForegroundNotification(
                    title = "Safe Drive Africa",
                    message = "Trip active — collecting location and sensor data"
                )
            )
        } catch (e: SecurityException) {
            Log.e("DataCollectionService", "Failed to start foreground service; stopping", e)
            if (!hasLocationPermission()) {
                notificationManager.displayPermissionNotification(
                    "Open the app and grant location permission to start data collection"
                )
            } else {
                Log.w("DataCollectionService", "Foreground start failed even though location permission is granted.")
            }
            serviceScope.launch {
                sensorRepo.updateCollectionStatus(false)
                sensorRepo.startTripStatus(false)
                sensorRepo.updateCurrentTripId(null)
            }
            stopSelf()
            return
        } catch (e: RuntimeException) {
            if (isForegroundStartAllowedException(e)) {
                Log.e("DataCollectionService", "Foreground start not allowed; stopping", e)
                notificationManager.displayPermissionNotification(
                    "Open the app to start data collection"
                )
                serviceScope.launch {
                    sensorRepo.updateCollectionStatus(false)
                    sensorRepo.startTripStatus(false)
                    sensorRepo.updateCurrentTripId(null)
                }
                stopSelf()
                return
            }
            throw e
        }

        serviceScope.launch {
            sensorRepo.startTripStatus(true)
            sensorRepo.updateCollectionStatus(true)
            sensorRepo.updateCurrentTripId(tripId)
        }

        serviceScope.launch {
            startDataCollection(tripId)
        }
    }

    override fun onDestroy() {
        Log.d("DataCollectionService", "Service destroying - starting cleanup")

        try {
            restoreJob?.cancel()
            val movementServiceRunning = isMovementServiceRunning()
            // Stop data collection
            stopDataCollection()
            serviceScope.launch {
                sensorRepo.updateCollectionStatus(false)
                sensorRepo.startTripStatus(false)
                sensorRepo.updateCurrentTripId(null)
            }

            // Stop foreground and clear notification
            stopForegroundCompat(!movementServiceRunning)

            // Cancel service scope
            serviceScope.cancel()

            Log.d("DataCollectionService", "Service destroyed successfully")
        } catch (e: Exception) {
            Log.e("DataCollectionService", "Error during service destruction", e)
        } finally {
            super.onDestroy()
        }
    }

    override fun onTaskRemoved(intent: Intent?) {
        Log.d("DataCollectionService", "Task removed - keeping background collection active")
        super.onTaskRemoved(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startDataCollection(tripId: UUID) {
        try {
            hardwareModule.startDataCollection(tripId)
        } catch (e: Exception) {
            Log.e("DataCollectionService", "Error starting data collection for trip $tripId", e)
            stopSelf()
        }
    }

    private fun stopDataCollection() {
        try {
            hardwareModule.stopDataCollection()
        } catch (e: Exception) {
            Log.e("DataCollectionService", "Error stopping data collection", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat(removeNotification: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val behavior = if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH
            stopForeground(behavior)
        } else {
            stopForeground(removeNotification)
        }
    }

    @Suppress("DEPRECATION")
    private fun isMovementServiceRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false
        val services = activityManager.getRunningServices(Int.MAX_VALUE)
        return services.any { it.service.className == VehicleMovementServiceUpdate::class.java.name }
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

    private fun isForegroundStartAllowedException(throwable: Throwable): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            throwable is android.app.ForegroundServiceStartNotAllowedException
    }
}
