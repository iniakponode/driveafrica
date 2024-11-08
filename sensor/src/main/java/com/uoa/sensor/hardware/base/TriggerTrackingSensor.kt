package com.uoa.sensor.hardware.base

interface TriggerTrackingSensor {
    val sensorType: Int
    var onTrigger: (() -> Unit)?

    fun doesSensorExist(): Boolean
    fun startListeningToSensor(): Boolean
    fun stopListeningToSensor()

    fun setOnTriggerListener(listener: () -> Unit) {
        onTrigger = listener
    }
}
