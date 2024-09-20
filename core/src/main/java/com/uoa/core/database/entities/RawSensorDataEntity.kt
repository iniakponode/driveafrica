package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.Date
import java.util.UUID

/**
 * Data class for holding sensor data.
 * @param sensorType Integer identifier for the type of sensor (e.g., accelerometer, gyroscope).
 * @param values List of float values representing the data from the sensor.
 * @param timestamp The exact time the data was recorded.
 * @param accuracy Sensor data accuracy level.
 */
@Entity(
    tableName = "raw_sensor_data",
    foreignKeys = [
        ForeignKey(entity = LocationEntity::class, parentColumns = ["id"], childColumns = ["locationId"]),
        ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"])
    ],
    indices = [Index(value = ["locationId"]), Index(value = ["tripId"]), Index(value = ["sync"]), Index(value = ["date"])]
    )
data class RawSensorDataEntity(
    @PrimaryKey (autoGenerate = false) var id: UUID,
    val sensorType: Int,
    val sensorTypeName: String,
    val values: List<Float>,
    val timestamp: Long,
    val date: Date?,
    val accuracy: Int,
    val locationId: UUID?,  // Foreign key to LocationEntity
    val tripId: UUID?,  // Foreign key to TripEntity
    var sync: Boolean=false
)

