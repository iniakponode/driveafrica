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
import javax.inject.Inject
import com.uoa.sensor.hardware.HardwareModule
import com.uoa.core.notifications.VehicleNotificationManager

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.O)
class VehicleMovementService : Service() {

    @Inject
    lateinit var hardwareModule: HardwareModule

    private lateinit var notificationManager: VehicleNotificationManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d("VehicleMovementService", "Service onCreate()")
        notificationManager = VehicleNotificationManager(this)

        // Start continuous movement detection.
        hardwareModule.startMovementDetection()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("VehicleMovementService", "Service onStartCommand()")
        startForeground(
            NOTIFICATION_ID,
            notificationManager.buildForegroundNotification(
                title = "Vehicle Movement Service",
                message = "Monitoring vehicle movement continuously"
            )
        )
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("VehicleMovementService", "Service onDestroy()")
        hardwareModule.stopMovementDetection()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 103
    }
}