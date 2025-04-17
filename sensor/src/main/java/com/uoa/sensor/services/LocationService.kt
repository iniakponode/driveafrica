//package com.uoa.sensor.services
//
//import android.app.Service
//import android.content.Intent
//import android.os.Build
//import android.os.IBinder
//import androidx.annotation.RequiresApi
//import com.uoa.sensor.location.LocationManager
//import com.uoa.core.notifications.VehicleNotificationManager
//import dagger.hilt.android.AndroidEntryPoint
//import javax.inject.Inject
//
//@RequiresApi(Build.VERSION_CODES.O)
//@AndroidEntryPoint
//class LocationService: Service() {
//    @Inject
//    lateinit var locationManager: LocationManager  // Inject LocationManager using Hilt
//
//    private lateinit var notificationManager: VehicleNotificationManager
//
//    override fun onCreate() {
//        super.onCreate()
//        notificationManager = VehicleNotificationManager(this)
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        // Start location updates using LocationManager
//        locationManager.startLocationUpdates()
//
//        // Start the service as a foreground service
//        startForeground(
//            NOTIFICATION_ID,  // Notification ID
//            notificationManager.buildForegroundNotification(
//                title = "Location Service",
//                message = "Location data is being collected in the background"
//            )  // Use a foreground notification
//        )
//
//        return START_STICKY
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        // Stop location updates when service is destroyed
//        locationManager.stopLocationUpdates()
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return null
//    }
//
//
//    companion object {
//        const val NOTIFICATION_ID = 1
//    }
//}