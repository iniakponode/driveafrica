package com.uoa.sensor.data.model


import java.time.Instant

/**
 * Data class for holding sensor data.
 * @param sensorType Integer identifier for the type of sensor (e.g., accelerometer, gyroscope).
 * @param values List of float values representing the data from the sensor.
 * @param timestamp The exact time the data was recorded.
 * @param accuracy Sensor data accuracy level.
// * @param location Optional location data, if required for the sensor data context.
 */
data class RawSensorData(
    val id: Int = 0,
    val sensorType: Int,
    val values: List<Float>,
    val timestamp: Instant,
    val accuracy: Int,
    val sync: Boolean = false
)