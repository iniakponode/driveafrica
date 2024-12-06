package com.uoa.sensor.notifications
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
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
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Use a relevant icon
            .setContentTitle(title)
            .setContentText(message)
            .setSilent(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true) // Persistent until cleared
            .build()
    }

    // Build the notification specifically for foreground service use
    fun buildForegroundNotification(title: String, message: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Use a relevant icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Lower priority for foreground services
            .setOngoing(true) // Required for foreground service
            .setSilent(false) // Optional: prevents sound/vibration for a silent notification
            .build()
    }

    // Create notification channel for Android O and above
    private fun createNotificationChannel() {
        val name = "Vehicle State Channel"
        val descriptionText = "Channel for vehicle movement state notifications"
        val importance = NotificationManager.IMPORTANCE_LOW // Suitable for background/foreground
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}