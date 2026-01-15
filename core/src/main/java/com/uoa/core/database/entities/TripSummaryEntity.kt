package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "trip_summary",
    indices = [
        Index(value = ["driverId"]),
        Index(value = ["startDate"]),
        Index(value = ["endDate"])
    ]
)
data class TripSummaryEntity(
    @PrimaryKey
    val tripId: UUID,
    val driverId: UUID,
    val startTime: Long,
    val endTime: Long,
    val startDate: Date,
    val endDate: Date,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val harshBrakingEvents: Int,
    val harshAccelerationEvents: Int,
    val speedingEvents: Int,
    val swervingEvents: Int,
    val classificationLabel: String,
    val alcoholProbability: Float?
)
