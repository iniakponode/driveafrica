//package com.uoa.sensor.services
//
//import android.app.Notification
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.content.Context
//import android.os.Build
//import androidx.core.app.NotificationCompat
//import com.uoa.sensor.R
//
//object NotificationHelper {
//
//    private const val CHANNEL_ID = "VehicleMovementServiceChannel"
//
//    fun createNotificationChannel(context: Context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                CHANNEL_ID,
//                "Vehicle Movement Service",
//                NotificationManager.IMPORTANCE_DEFAULT
//            )
//            val manager = context.getSystemService(NotificationManager::class.java)
//            manager.createNotificationChannel(channel)
//        }
//    }
//
//    fun createNotification(context: Context): Notification {
//        return NotificationCompat.Builder(context, CHANNEL_ID)
//            .setContentTitle("Safe Drive Africa")
//            .setContentText("Collecting trip data")
//            .setSmallIcon(R.drawable.ic_car)
//            .build()
//    }
//}