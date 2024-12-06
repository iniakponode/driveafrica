package com.uoa.nlgengine.data.model

import com.uoa.core.model.Trip
import kotlinx.datetime.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration

data class ReportStatistics(

    // Common fields for all PeriodTypes
    val totalIncidences: Int=0,
    val mostFrequentUnsafeBehaviour: String?=null,
    val mostFrequentBehaviourCount: Int = 0,
    val mostFrequentBehaviourOccurrences: List<BehaviourOccurrence> = emptyList(), // New field
    val tripWithMostIncidences: Trip?=null,
    val tripsPerAggregationUnit: Map<java.time.LocalDate, Int> = emptyMap(),
    val aggregationUnitWithMostIncidences: LocalDate?=null,
    val incidencesPerAggregationUnit:  Map<LocalDate, Int> = emptyMap(),
    val incidencesPerTrip: Map<UUID, Int> = emptyMap(),
    val aggregationLevel: AggregationLevel?=null,
    val aggregationUnitsWithAlcoholInfluence: Int=0,
    val tripsWithAlcoholInfluencePerAggregationUnit: Map<java.time.LocalDate, Int> = emptyMap(),

    val numberOfTrips: Int=0,
    val numberOfTripsWithIncidences: Int=0,
    val numberOfTripsWithAlcoholInfluence: Int=0,


    // Fields specific to LAST_TRIP
    val lastTripDuration: Duration? = null,
    val lastTripDistance: Double? = null,
    val lastTripAverageSpeed: Double? = null,
    val lastTripStartLocation: String? = null,
    val lastTripEndLocation: String? = null,
    val lastTripStartTime: LocalDateTime? = null,
    val lastTripEndTime: LocalDateTime? = null,
    val lastTripInfluence: String? = null
)

