package com.uoa.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.uoa.core.R
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class VehicleNotificationManager(private val context: Context) {

    private val CHANNEL_ID = "vehicle_state_channel"
    private val LOCATION_SERVICE_CHANNEL_ID = "location_service_channel"
    private val NOTIFICATION_ID = 1001


    init {
        createNotificationChannel()
    }

    // Display notification based on the message provided
    fun displayNotification(title: String, message: String) {
        val notification = buildNotification(title, message)
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    // Clear the notification
    fun clearNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    // Build the notification with the given message (for general purposes)
    private fun buildNotification(title: String, message: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_ic) // Use a relevant icon
            .setContentTitle(title)
            .setContentText(message)
            .setSilent(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true) // Persistent until cleared
            .build()
    }


    // Build the notification specifically for foreground service use
    fun buildForegroundNotification(title: String, message: String): Notification {
       val  largeIcon: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.sda_2);
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_ic)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Lower priority for foreground services
            .setOngoing(true) // Required for foreground service
            .setSilent(false) // Optional: prevents sound/vibration for a silent notification
            .build()
    }

    // Create notification channel for Android O and above
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "vehicle_state_channel"
            val channelName = "Vehicle Movement Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Channel for vehicle movement notifications"
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}