package com.uoa.core.model

import java.util.Date
import java.util.UUID

data class ReportStatistics(
    val id: UUID,
    val driverProfileId: UUID,
    val tripId: UUID? = null,
    val createdDate: java.time.LocalDate,
    val startDate: java.time.LocalDate?,
    val endDate: java.time.LocalDate?,

    val totalIncidences: Int = 0,
    val mostFrequentUnsafeBehaviour: String? = null,
    val mostFrequentBehaviourCount: Int = 0,

    val mostFrequentBehaviourOccurrences: List<BehaviourOccurrence> = emptyList(),
    val tripWithMostIncidences: Trip? = null,
    val tripsPerAggregationUnit: Map<java.time.LocalDate, Int> = emptyMap(),
    val aggregationUnitWithMostIncidences: java.time.LocalDate? = null,
    val incidencesPerAggregationUnit: Map<java.time.LocalDate, Int> = emptyMap(),
    val incidencesPerTrip: Map<UUID, Int> = emptyMap(),
    val aggregationLevel: AggregationLevel? = null,
    val aggregationUnitsWithAlcoholInfluence: Int = 0,
    val tripsWithAlcoholInfluencePerAggregationUnit: Map<java.time.LocalDate, Int> = emptyMap(),
    val sync: Boolean = false,
    val processed: Boolean = false,

    val numberOfTrips: Int = 0,
    val numberOfTripsWithIncidences: Int = 0,
    val numberOfTripsWithAlcoholInfluence: Int = 0,

    val lastTripDuration: java.time.Duration? = null,
    val lastTripDistance: Double? = null,
    val lastTripAverageSpeed: Double? = null,
    val lastTripStartLocation: String? = null,
    val lastTripEndLocation: String? = null,
    val lastTripStartTime: java.time.LocalDateTime? = null,
    val lastTripEndTime: java.time.LocalDateTime? = null,
    val lastTripInfluence: String? = null
)


