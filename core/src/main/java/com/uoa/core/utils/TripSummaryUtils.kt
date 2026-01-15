package com.uoa.core.utils

import com.uoa.core.model.LocationData
import com.uoa.core.model.Trip
import com.uoa.core.model.TripSummary
import com.uoa.core.model.UnsafeBehaviourModel
import java.util.Date
import kotlin.math.max

fun buildTripSummary(
    trip: Trip,
    locations: List<LocationData>,
    unsafeBehaviours: List<UnsafeBehaviourModel>
): TripSummary {
    val endTime = trip.endTime ?: System.currentTimeMillis()
    val endDate = trip.endDate ?: Date(endTime)
    val startDate = trip.startDate ?: Date(trip.startTime)
    val distanceMeters = locations.sumOf { it.distance ?: 0.0 }
    val durationSeconds = max(0L, (endTime - trip.startTime) / 1000L)

    val harshBraking = unsafeBehaviours.count { it.behaviorType.equals("Harsh Braking", ignoreCase = true) }
    val harshAcceleration = unsafeBehaviours.count { it.behaviorType.equals("Harsh Acceleration", ignoreCase = true) }
    val speeding = unsafeBehaviours.count { it.behaviorType.equals("Speeding", ignoreCase = true) }
    val swerving = unsafeBehaviours.count { it.behaviorType.equals("Swerving", ignoreCase = true) }

    return TripSummary(
        tripId = trip.id,
        driverId = trip.driverPId ?: throw IllegalStateException("Trip ${trip.id} missing driver id"),
        startTime = trip.startTime,
        endTime = endTime,
        startDate = startDate,
        endDate = endDate,
        distanceMeters = distanceMeters,
        durationSeconds = durationSeconds,
        harshBrakingEvents = harshBraking,
        harshAccelerationEvents = harshAcceleration,
        speedingEvents = speeding,
        swervingEvents = swerving,
        classificationLabel = trip.influence ?: "Unknown",
        alcoholProbability = trip.alcoholProbability
    )
}
