package com.uoa.core.model

import java.util.Date
import java.util.UUID

/**
 * Data class for holding sensor data.
 * @param sensorType Integer identifier for the type of sensor (e.g., accelerometer, gyroscope).
 * @param values List of float values representing the data from the sensor.
 * @param timestamp The exact time the data was recorded.
 * @param accuracy Sensor data accuracy level.
// * @param location Optional location data, if required for the sensor data context.
 */
data class RawSensorData(
    val id: UUID,
    val sensorType: Int,
    val sensorTypeName: String,
    val values: List<Float>,
    val timestamp: Long,
    val date: Date?,
    val accuracy: Int,
    val locationId: UUID? = null,  // Foreign key to LocationEntity
    val tripId: UUID? = null,  // Foreign key to TripEntity
    val sync: Boolean = false
)