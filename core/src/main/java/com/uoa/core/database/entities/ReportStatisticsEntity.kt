package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.uoa.core.model.AggregationLevel
import com.uoa.core.model.BehaviourOccurrence
import com.uoa.core.model.Trip
import kotlinx.datetime.LocalDate
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID
import kotlin.time.Duration

@Entity(
    tableName = "report_statistics",
    foreignKeys = [
        ForeignKey(
            entity = DriverProfileEntity::class,
            parentColumns = ["driverProfileId"],
            childColumns = ["driverProfileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["startDate"]),
        Index(value = ["endDate"]),
        Index(value = ["driverProfileId"]),
        Index(value = ["id"], unique = true)
    ]
)
data class ReportStatisticsEntity(
    @PrimaryKey(autoGenerate = false)
    val id: UUID,
    val driverProfileId: UUID,
    val tripId: UUID? = null,
    val createdDate: java.time.LocalDate,

    // Use a single date/time library (java.time) consistently
    val startDate: java.time.LocalDate?,
    val endDate: java.time.LocalDate?,

    val totalIncidences: Int = 0,
    val mostFrequentUnsafeBehaviour: String? = null,
    val mostFrequentBehaviourCount: Int = 0,

    // Store complex fields as JSON strings and provide TypeConverters
    val mostFrequentBehaviourOccurrences: String = "[]", // JSON string
    val tripWithMostIncidences: String? = null, // JSON string representing Trip
    val tripsPerAggregationUnit: String = "{}", // JSON string for Map<LocalDate, Int>
    val aggregationUnitWithMostIncidences: java.time.LocalDate? = null, // unified to java.time.LocalDate
    val incidencesPerAggregationUnit: String = "{}", // JSON string for Map<LocalDate, Int>
    val incidencesPerTrip: String = "{}", // JSON string for Map<UUID, Int>
    val aggregationLevel: AggregationLevel? = null,
    val aggregationUnitsWithAlcoholInfluence: Int = 0,
    val tripsWithAlcoholInfluencePerAggregationUnit: String = "{}", // JSON string for Map<LocalDate, Int>
    val sync: Boolean = false,
    val processed: Boolean = false,

    val numberOfTrips: Int = 0,
    val numberOfTripsWithIncidences: Int = 0,
    val numberOfTripsWithAlcoholInfluence: Int = 0,

    // For the last trip fields, if they are complex or domain-specific, also store as strings or simpler types with converters
    val lastTripDuration: String? = null, // Convert Duration to String (e.g., ISO-8601) with a converter
    val lastTripDistance: Double? = null,
    val lastTripAverageSpeed: Double? = null,
    val lastTripStartLocation: String? = null,
    val lastTripEndLocation: String? = null,
    val lastTripStartTime: LocalDateTime? = null, // java.time.LocalDateTime is fine if you have a converter
    val lastTripEndTime: LocalDateTime? = null,
    val lastTripInfluence: String? = null
)

