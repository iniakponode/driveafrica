package com.uoa.core.apiServices.models.reportStatisticsModels


import com.uoa.core.model.AggregationLevel
import com.uoa.core.model.BehaviourOccurrence
import com.uoa.core.model.Trip
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration

data class ReportStatisticsCreate(
    val id: UUID,
    val driverProfileId: UUID,
    val tripId: UUID? = null,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val createdDate: LocalDate,
    val totalIncidences: Int = 0,
    val mostFrequentUnsafeBehaviour: String? = null,
    val mostFrequentBehaviourCount: Int = 0,
    val mostFrequentBehaviourOccurrences: List<BehaviourOccurrence> = emptyList(),
    val tripWithMostIncidences: Trip? = null,
    val tripsPerAggregationUnit: Map<LocalDate, Int> = emptyMap(),
    val aggregationUnitWithMostIncidences: LocalDate? = null,
    val incidencesPerAggregationUnit: Map<LocalDate, Int> = emptyMap(),
    val incidencesPerTrip: Map<UUID, Int> = emptyMap(),
    val aggregationLevel: AggregationLevel? = null,
    val aggregationUnitsWithAlcoholInfluence: Int = 0,
    val tripsWithAlcoholInfluencePerAggregationUnit: Map<LocalDate, Int> = emptyMap(),
    val sync: Boolean = false,
    val processed: Boolean = false,
    val numberOfTrips: Int = 0,
    val numberOfTripsWithIncidences: Int = 0,
    val numberOfTripsWithAlcoholInfluence: Int = 0,
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

