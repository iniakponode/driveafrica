package com.uoa.sensor.hardware.base

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.util.Log

open class SignificantMotionSensor(
    private val context: Context
) : TriggerTrackingSensor {

    override val sensorType: Int = Sensor.TYPE_SIGNIFICANT_MOTION
    override var onTrigger: (() -> Unit)? = null

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var sensor: Sensor? = null

    private val triggerEventListener = object : TriggerEventListener() {
        override fun onTrigger(event: TriggerEvent?) {
            event?.let {
                Log.d("SignificantMotionSensor", "Significant motion detected.")
                onTrigger?.invoke()
            }
            // Re-register the sensor if you want to detect future events
            val success = startListeningToSensor()
            Log.d("SignificantMotionSensor", "Listener re-registered: $success")
        }
    }

    override fun doesSensorExist(): Boolean {
        val exists = sensorManager.getDefaultSensor(sensorType) != null
        Log.d("SignificantMotionSensor", "Sensor exists: $exists")
        return exists
    }

    override fun startListeningToSensor(): Boolean {
        if (!doesSensorExist()) {
            Log.e("SignificantMotionSensor", "Significant Motion Sensor not available on this device.")
            return false
        }
        sensor = sensorManager.getDefaultSensor(sensorType)
        val success = sensorManager.requestTriggerSensor(triggerEventListener, sensor)
        Log.d("SignificantMotionSensor", "Listener registered: $success")
        return success
    }

    override fun stopListeningToSensor() {
        if (!doesSensorExist() || sensor == null) {
            Log.e("SignificantMotionSensor", "Cannot stop listener; sensor is null or does not exist.")
            return
        }
        val success = sensorManager.cancelTriggerSensor(triggerEventListener, sensor)
        Log.d("SignificantMotionSensor", "Listener unregistered: $success")
    }

    override fun setOnTriggerListener(listener: () -> Unit) {
        onTrigger = listener
    }
}