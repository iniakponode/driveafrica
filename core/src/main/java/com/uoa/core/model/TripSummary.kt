package com.uoa.core.model

import java.util.Date
import java.util.UUID

data class TripSummary(
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
