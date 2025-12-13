package com.uoa.sensor.services

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.uoa.core.notifications.VehicleNotificationManager
import com.uoa.sensor.hardware.HardwareModule

@AndroidEntryPoint
class DataCollectionService : Service() {

    @Inject
    lateinit var hardwareModule: HardwareModule

    private lateinit var notificationManager: VehicleNotificationManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        notificationManager = VehicleNotificationManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val tripIdString = intent?.getStringExtra("TRIP_ID")
        val tripId = tripIdString?.let {
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

        if (tripId == null) {
            Log.e("DataCollectionService", "No valid Trip ID provided. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            NOTIFICATION_ID,
            notificationManager.buildForegroundNotification(
                title = "Sensor and Location Data Collection Service",
                message = "Sensors and Location Data collection is started and ongoing"
            )
        )

        serviceScope.launch {
            startDataCollection(tripId)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("DataCollectionService", "Service destroying - starting cleanup")

        try {
            // Stop data collection
            stopDataCollection()

            // Complete cleanup to prevent memory leaks
            hardwareModule.cleanup()

            // Stop foreground and clear notification
            stopForeground(true)
            notificationManager.clearNotification()

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
        Log.d("DataCollectionService", "Task removed - cleaning up")

        try {
            stopDataCollection()
            hardwareModule.cleanup()
        } catch (e: Exception) {
            Log.e("DataCollectionService", "Error in onTaskRemoved", e)
        }

        stopSelf()
        super.onTaskRemoved(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startDataCollection(tripId: UUID) {
        try {
            hardwareModule.startDataCollection(tripId)
            notificationManager.displayNotification(
                "Sensors and Location Data Collection Service",
                "Collecting Sensors and Location Data for trip: $tripId"
            )
        } catch (e: Exception) {
            Log.e("DataCollectionService", "Error starting data collection for trip $tripId", e)
            stopSelf()
        }
    }

    private fun stopDataCollection() {
        try {
            hardwareModule.stopDataCollection()
            stopForeground(true)
            notificationManager.clearNotification()
        } catch (e: Exception) {
            Log.e("DataCollectionService", "Error stopping data collection", e)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}