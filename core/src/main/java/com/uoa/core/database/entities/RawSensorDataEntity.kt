package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Data class for holding sensor data.
 * @param sensorType Integer identifier for the type of sensor (e.g., accelerometer, gyroscope).
 * @param values List of float values representing the data from the sensor.
 * @param timestamp The exact time the data was recorded.
 * @param accuracy Sensor data accuracy level.
 * @param location Optional location data, if required for the sensor data context.
 */
@Entity(
    tableName = "raw_sensor_data"
    )
data class RawSensorDataEntity(
    @PrimaryKey (autoGenerate = true) var id: Int,
    val sensorType: String,
    val values: List<Float>,
    val timestamp: Instant,
    val accuracy: Int,
    var sync: Boolean=false
)

