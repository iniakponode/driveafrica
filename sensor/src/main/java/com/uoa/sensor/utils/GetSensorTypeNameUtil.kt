package com.uoa.sensor.utils

import android.hardware.Sensor

object GetSensorTypeNameUtil {
    fun getSensorTypeName(sensorType: Int): String {
        return when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Acceleration"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            Sensor.TYPE_ROTATION_VECTOR -> "Rotation Vector"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetometer"
            Sensor.TYPE_GRAVITY -> "Gravity"
            else -> "Unknown"
        }
    }
}