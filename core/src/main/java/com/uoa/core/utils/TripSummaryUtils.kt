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

    val canonicalTypes = mapOf(
        "harsh braking" to "Harsh Braking",
        "harsh acceleration" to "Harsh Acceleration",
        "speeding" to "Speeding",
        "swerving" to "Swerving",
        "aggressive turn" to "Aggressive Turn",
        "aggressive stop-and-go" to "Aggressive Stop-and-Go",
        "phone handling" to "Phone Handling",
        "fatigue" to "Fatigue",
        "rough road speeding" to "Rough Road Speeding",
        "crash detected" to "Crash Detected"
    )

    val behaviourCounts = mutableMapOf<String, Int>()
    unsafeBehaviours.forEach { behaviour ->
        val rawType = behaviour.behaviorType.trim()
        val normalizedKey = canonicalTypes[rawType.lowercase()] ?: rawType
        behaviourCounts[normalizedKey] = (behaviourCounts[normalizedKey] ?: 0) + 1
    }

    return TripSummary(
        tripId = trip.id,
        driverId = trip.driverPId ?: throw IllegalStateException("Trip ${trip.id} missing driver id"),
        startTime = trip.startTime,
        endTime = endTime,
        startDate = startDate,
        endDate = endDate,
        distanceMeters = distanceMeters,
        durationSeconds = durationSeconds,
        unsafeBehaviourCounts = behaviourCounts,
        classificationLabel = trip.influence ?: "Unknown",
        alcoholProbability = trip.alcoholProbability
    )
}
