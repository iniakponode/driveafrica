package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.base.Objects
import java.util.Date
import java.util.UUID

/**
 * Optional class for location data if needed to pair with sensor data.
 * @param latitude The latitude of the location.
 * @param longitude The longitude of the location.
 * @param altitude The altitude of the location (if available).
 * @param speed The speed at the location (if available).
 */
@Entity(tableName = "location",
    indices = [
        Index(
            value=["id"],
            unique = true
        )
    ])
data class LocationEntity(
    @PrimaryKey val id: UUID,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val date: Date?,
    val altitude: Double,
    val speed: Float,
    val distance: Float,
    val processed: Boolean= false,
    var sync: Boolean
)