package com.uoa.sensor.hardware.base

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.time.Instant
import javax.inject.Inject

abstract class AndroidSensor(
    private val context: Context,
    sensorType: Int
) : TrackingSensor(sensorType), SensorEventListener {

    private var sensorManager: SensorManager
    private var sensor: Sensor? = null

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun doesSensorExist(): Boolean {
        return sensorManager.getDefaultSensor(sensorType) != null
    }

    override fun startListeningToSensor(rate: Int): Boolean {
        if (!doesSensorExist()) return false
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val defaultSensor = sensorManager.getDefaultSensor(sensorType)
        if (defaultSensor != null) {
            sensor = defaultSensor
            return sensorManager.registerListener(this, sensor, rate)
        }
        return false
    }

    override fun stopListeningToSensor() {
        if (!doesSensorExist() || sensor == null) return
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == sensorType) {
                whenSensorValueChanges?.invoke(sensorType, it.values.toList(), it.accuracy)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (!doesSensorExist() || sensor != this.sensor) return
        onAccuracyValueChanged?.invoke(accuracy)
    }
}