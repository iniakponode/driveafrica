package com.uoa.sensor.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.uoa.sensor.hardware.HardwareModule
import com.uoa.sensor.repository.SensorDataColStateRepository
import com.uoa.sensor.notifications.VehicleNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.util.UUID

//@HiltWorker
//class SensorWorker @AssistedInject constructor(
//    @Assisted context: Context,
//    @Assisted workerParams: WorkerParameters,
//    private val sensorDataCollectionRepo: SensorDataColStateRepository,
//    private val hardwareModule: HardwareModule
//) : CoroutineWorker(context, workerParams) {
//
//    private val notificationManager = VehicleNotificationManager(context)
//
//    // Track last state to avoid repeated notifications
//    private var lastVehicleMovingState: Boolean? = null
//    private var sensorsStoppedNotified = false
//
//    // Define a notification ID for this foreground service
//    private val notificationId = 1
//
//    override suspend fun doWork(): Result {
//        // Start the worker as a foreground service
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            setForeground(getForegroundInfo())
//        }
//
//        val taskType = inputData.getString("TASK_TYPE")
//        val tripId = inputData.getString("TRIP_ID")
//        val tripIdUUID = UUID.fromString(tripId)
//
//        return when (taskType) {
//            "START" -> {
//                hardwareModule.startDataCollection(tripIdUUID)
//
//                try {
//                    // Start monitoring vehicle movement in a coroutine
//                    monitorVehicleMovementState()
//
//                    // Continuous data collection simulation while the worker is active
//                    while (!isStopped) {
//                        delay(1000) // Simulate ongoing data collection
//                    }
//                } catch (e: InterruptedException) {
//                    return Result.failure()
//                } finally {
//                    // Notify user that sensors are stopped and clear other notifications
//                    if (!sensorsStoppedNotified) {
//                        notificationManager.displayNotification("Sensors State","Sensors stopped. No data collection in progress.")
//                        sensorsStoppedNotified = true
//                    } else {
//                        notificationManager.clearNotification()
//                    }
//                }
//
//                Result.success()
//            }
//
//            else -> Result.failure()
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    override suspend fun getForegroundInfo(): ForegroundInfo {
//        // Build a notification to represent the ongoing sensor collection
//        val notification = notificationManager.buildForegroundNotification(
//            title = "Sensors in Background",
//            message = "Sensors are running\nto collect data in the background...."
//        )
//        val serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
//        // Return a ForegroundInfo object with the notification
//        return ForegroundInfo(notificationId, notification,serviceType)
//    }
//
//    private suspend fun monitorVehicleMovementState() {
//        // Launch a coroutine to observe the isVehicleMoving state
//        while (!isStopped) {
//            val isVehicleMoving = sensorDataCollectionRepo.isVehicleMoving.value
//
//            // Notify only when there's a change in state
//            if (lastVehicleMovingState != isVehicleMoving) {
//                lastVehicleMovingState = isVehicleMoving
//
//                if (isVehicleMoving) {
//                    // Notify once when vehicle starts moving
//                    notificationManager.displayNotification("Vehicle State","Sensor data collection has started.")
//                } else {
//                    // Notify once when waiting for vehicle to move
//                    notificationManager.displayNotification("Vehicle State","Waiting for vehicle to start moving to collect data.")
//                }
//            }
//
//            // Check state every 5 seconds
//            delay(5000)
//        }
//    }
//}
//
