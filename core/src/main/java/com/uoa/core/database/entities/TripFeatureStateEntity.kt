package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "trip_feature_state",
    indices = [
        Index(value = ["tripId"], unique = true),
        Index(value = ["driverProfileId"])
    ]
)
data class TripFeatureStateEntity(
    @PrimaryKey val tripId: UUID,
    val driverProfileId: UUID? = null,
    val accelCount: Int = 0,
    val accelMean: Double = 0.0,
    val speedCount: Int = 0,
    val speedMean: Double = 0.0,
    val speedM2: Double = 0.0,
    val courseCount: Int = 0,
    val courseMean: Double = 0.0,
    val courseM2: Double = 0.0,
    val lastLocationId: UUID? = null,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val lastLocationTimestamp: Long? = null,
    val lastSensorTimestamp: Long? = null,
    val sync: Boolean = false
)
