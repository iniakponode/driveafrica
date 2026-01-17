package com.uoa.core.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.uoa.core.R
import com.uoa.core.utils.Constants
import com.uoa.core.utils.SENSOR_CONTROL_SCREEN_ROUTE

class VehicleNotificationManager(private val context: Context) {

    private val CHANNEL_ID = "vehicle_state_channel"
    private val UPLOAD_CHANNEL_ID = "upload_status_channel"
    private val TRIP_EVENT_CHANNEL_ID = "trip_event_channel"
    private val NOTIFICATION_ID = 1001
    private val UPLOAD_NOTIFICATION_ID = 1002
    private val TRIP_EVENT_NOTIFICATION_ID = 1003

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 2001
        private const val TAG = "VehicleNotificationManager"
    }


    init {
        createNotificationChannel()
    }

    // Display notification based on the message provided
    fun displayNotification(title: String, message: String) {
        val notification = buildNotification(title, message)
        notifyIfAllowed(NOTIFICATION_ID, notification)
    }

    fun displayUploadStatus(
        title: String,
        message: String,
        progress: Int? = null,
        max: Int? = null,
        ongoing: Boolean = true
    ) {
        val notification = buildStatusNotification(
            channelId = UPLOAD_CHANNEL_ID,
            title = title,
            message = message,
            ongoing = ongoing,
            autoCancel = !ongoing,
            progress = progress,
            max = max
        )
        notifyIfAllowed(UPLOAD_NOTIFICATION_ID, notification)
    }

    fun displayUploadFailure(message: String) {
        val notification = buildStatusNotification(
            channelId = UPLOAD_CHANNEL_ID,
            title = "Upload failed",
            message = message,
            ongoing = false,
            autoCancel = true,
            progress = null,
            max = null
        )
        notifyIfAllowed(UPLOAD_NOTIFICATION_ID, notification)
    }

    fun displayUploadComplete(message: String) {
        val notification = buildStatusNotification(
            channelId = UPLOAD_CHANNEL_ID,
            title = "Upload complete",
            message = message,
            ongoing = false,
            autoCancel = true,
            progress = null,
            max = null
        )
        notifyIfAllowed(UPLOAD_NOTIFICATION_ID, notification)
    }

    fun displayTripEvent(title: String, message: String) {
        val notification = buildStatusNotification(
            channelId = TRIP_EVENT_CHANNEL_ID,
            title = title,
            message = message,
            ongoing = false,
            autoCancel = true,
            progress = null,
            max = null
        )
        notifyIfAllowed(TRIP_EVENT_NOTIFICATION_ID, notification)
    }

    fun displayPermissionNotification(message: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply {
                putExtra(Constants.EXTRA_NAVIGATE_ROUTE, SENSOR_CONTROL_SCREEN_ROUTE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        val notification = if (launchIntent != null) {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, flags)
            buildNotification(
                title = "Action required",
                message = message,
                contentIntent = pendingIntent,
                ongoing = false
            )
        } else {
            buildNotification(title = "Action required", message = message, ongoing = false)
        }
        notifyIfAllowed(NOTIFICATION_ID, notification)
    }

    // Clear the notification
    fun clearNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun notifyIfAllowed(notificationId: Int, notification: Notification) {
        if (!hasPostNotificationsPermission()) {
            return
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (securityException: SecurityException) {
            Log.w(TAG, "Unable to post notification.", securityException)
        }
    }

    private fun hasPostNotificationsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Build the notification with the given message (for general purposes)
    private fun buildNotification(
        title: String,
        message: String,
        contentIntent: PendingIntent? = null,
        ongoing: Boolean = true
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setContentTitle(title)
            setContentText(message)
            setSilent(false)
            setOnlyAlertOnce(true)
            setPriority(NotificationCompat.PRIORITY_HIGH)
            setOngoing(ongoing)
            setSmallIcon(R.drawable.ic_stat_ic)
            contentIntent?.let { setContentIntent(it) }
        }.build()
    }

    private fun buildStatusNotification(
        channelId: String,
        title: String,
        message: String,
        ongoing: Boolean,
        autoCancel: Boolean,
        progress: Int?,
        max: Int?
    ): Notification {
        return NotificationCompat.Builder(context, channelId).apply {
            setContentTitle(title)
            setContentText(message)
            setSilent(true)
            setOnlyAlertOnce(true)
            setPriority(NotificationCompat.PRIORITY_LOW)
            setOngoing(ongoing)
            setAutoCancel(autoCancel)
            setSmallIcon(R.drawable.ic_stat_ic)
            if (progress != null && max != null && max > 0) {
                setProgress(max, progress.coerceAtMost(max), false)
            }
        }.build()
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
            .setSilent(true)
            .setOnlyAlertOnce(true)
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
            val uploadChannel = NotificationChannel(
                UPLOAD_CHANNEL_ID,
                "Upload Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for background upload status updates"
            }
            val tripEventChannel = NotificationChannel(
                TRIP_EVENT_CHANNEL_ID,
                "Trip Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for trip start and stop updates"
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(uploadChannel)
            notificationManager.createNotificationChannel(tripEventChannel)
        }
    }
}
