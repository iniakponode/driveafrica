package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Optional class for location data if needed to pair with sensor data.
 * @param latitude The latitude of the location.
 * @param longitude The longitude of the location.
 * @param altitude The altitude of the location (if available).
 * @param speed The speed at the location (if available).
 */
@Entity(tableName = "location")
data class LocationEntity(
    @PrimaryKey(autoGenerate = false) val id: UUID,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val altitude: Double? = null,
    val speed: Float? = null,
    val sync: Boolean=false
)