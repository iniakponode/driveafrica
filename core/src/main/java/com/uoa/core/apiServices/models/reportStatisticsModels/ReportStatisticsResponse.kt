package com.uoa.core.apiServices.models.reportStatisticsModels

import java.time.LocalDate
import java.util.UUID
import com.uoa.core.model.AggregationLevel
import com.uoa.core.model.BehaviourOccurrence
import com.uoa.core.model.Trip
import java.time.LocalDateTime
import kotlin.time.Duration

data class ReportStatisticsResponse(
    val id: UUID,
    val driverProfileId: UUID,
    val tripId: UUID? = null,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val createdDate: LocalDate,
    val totalIncidences: Int,
    val mostFrequentUnsafeBehaviour: String? = null,
    val mostFrequentBehaviourCount: Int,
    val mostFrequentBehaviourOccurrences: List<BehaviourOccurrence>,
    val tripWithMostIncidences: Trip? = null,
    val tripsPerAggregationUnit: Map<LocalDate, Int>,
    val aggregationUnitWithMostIncidences: LocalDate? = null,
    val incidencesPerAggregationUnit: Map<LocalDate, Int>,
    val incidencesPerTrip: Map<UUID, Int>,
    val aggregationLevel: AggregationLevel? = null,
    val aggregationUnitsWithAlcoholInfluence: Int,
    val tripsWithAlcoholInfluencePerAggregationUnit: Map<LocalDate, Int>,
    val sync: Boolean,
    val processed: Boolean,
    val numberOfTrips: Int,
    val numberOfTripsWithIncidences: Int,
    val numberOfTripsWithAlcoholInfluence: Int,
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

