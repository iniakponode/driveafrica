package com.uoa.sensor.services

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.uoa.sensor.hardware.HardwareModule
import com.uoa.core.notifications.VehicleNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.UUID
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
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
        val tripId = tripIdString?.let { UUID.fromString(it) }

        if (tripId != null) {
            // Launching the data collection start within the serviceScope
//            serviceScope.launch {
                startDataCollection(tripId)
//            }
        } else {
//            Log.e("DataCollectionService", "Trip ID is null")
            stopSelf()  // Gracefully stop the service if no Trip ID is found
        }

        // Start the service as a foreground service
        startForeground(
            NOTIFICATION_ID,
            notificationManager.buildForegroundNotification(
                title = "Data Collection Service",
                message = "Collecting location and sensor data in the background"
            )
        )

        return START_STICKY
    }

    override fun onDestroy() {
        stopDataCollection()
        super.onDestroy()
//        serviceScope.cancel()  // Cancel any ongoing coroutines to prevent memory leaks
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null  // No binding needed for this service
    }

    /**
     * Initiate Hybrid Motion Detection using HardwareModule
     */
    private fun startDataCollection(tripId: UUID) {
        try {
//            Log.d("DataCollectionService", "Initiating hybrid motion detection for trip: $tripId")

            hardwareModule.startDataCollection(tripId)
            notificationManager.displayNotification("Data Collection Service", "Data collection service started for trip: $tripId")
        } catch (e: Exception) {
            Log.e("DataCollectionService", "Error initiating hybrid motion detection", e)
        }
    }
    private fun stopDataCollection(){
//        Log.d("DataService", "Call to end service started")
        hardwareModule.stopDataCollection()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}





