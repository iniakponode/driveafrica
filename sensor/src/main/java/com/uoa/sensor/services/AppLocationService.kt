package com.uoa.sensor.services

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi

import com.uoa.sensor.location.LocationManager
import com.uoa.sensor.notifications.VehicleNotificationManager
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class AppLocationService : Service() {

    @Inject lateinit var locationManager: LocationManager  // Inject LocationManager using Hilt
    private lateinit var notificationManager: VehicleNotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = VehicleNotificationManager(this)

        // Start location updates using LocationManager
        locationManager.startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start the service as a foreground service
        startForeground(
            1,  // Notification ID
            notificationManager.buildForegroundNotification(
                title = "Location Service",
                message = "Location data\nis being collected in the background"
            )  // Use a foreground notification
        )

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop location updates when service is destroyed
        locationManager.stopLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
