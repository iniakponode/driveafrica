//// NotificationController.kt
//package com.uoa.sensor.notifications
//
//import android.content.Context
//import com.uoa.sensor.hardware.HardwareModule
//import com.uoa.sensor.hardware.VehicleStateObserver
//
//class NotificationController(
//    private val context: Context,
//    private val hardwareModule: HardwareModule
//) : VehicleStateObserver {
//
//    private val notificationManager = VehicleNotificationManager(context)
//
//    init {
//        // Register this controller as an observer in HardwareModule
//        hardwareModule.addVehicleStateObserver(this)
//    }
//
//    // Respond to state changes from HardwareModule
//    override fun onVehicleStateChanged(isVehicleMoving: Boolean) {
//        if (isVehicleMoving) {
//            notificationManager.displayNotification("Sensor data collection has started.")
//        } else {
//            notificationManager.displayNotification("Waiting for vehicle to start moving to collect data.")
//        }
//    }
//
//    // Cleanup if needed
//    fun removeObserver() {
//        hardwareModule.removeVehicleStateObserver(this)
//    }
//}
