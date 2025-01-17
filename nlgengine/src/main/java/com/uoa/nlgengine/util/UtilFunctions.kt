package com.uoa.nlgengine.util

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.model.ReportStatistics
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.model.LocationData
import com.uoa.core.network.apiservices.OSMApiService
import com.uoa.core.model.AggregationLevel
import com.uoa.core.model.BehaviourOccurrence
import com.uoa.core.model.getAggregationLevel
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.PeriodType
import com.uoa.core.utils.PeriodUtils
import com.uoa.nlgengine.domain.usecases.local.GetLastInsertedUnsafeBehaviourUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.HttpException
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.*
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.toKotlinDuration

suspend fun computeReportStatistics(
    context: Context,
    osmApiService: OSMApiService,
    startDate: LocalDate,
    endDate: LocalDate,
    periodType: PeriodType,
    unsafeBehaviours: List<UnsafeBehaviourModel>,
    tripRepository: TripDataRepository,
    locationRepository: LocationRepository,
    lastInsertedUnsafeBehaviour: GetLastInsertedUnsafeBehaviourUseCase
): ReportStatistics? {
    // If no unsafe behaviours for non-LAST_TRIP types, return null
    if (unsafeBehaviours.isEmpty() && periodType != PeriodType.LAST_TRIP) {
        return null
    }

    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val profileIdString = prefs.getString(DRIVER_PROFILE_ID, null) ?: return null
    val driverProfileId = UUID.fromString(profileIdString)

    // Common helper to extract most frequent behaviour info
    fun computeMostFrequentBehaviourInfo(behaviours: List<UnsafeBehaviourModel>): Pair<String?, Int> {
        val behaviourCounts = behaviours.groupingBy { it.behaviorType }.eachCount()
        val maxEntry = behaviourCounts.maxByOrNull { it.value }
        return maxEntry?.key to (maxEntry?.value ?: 0)
    }

    // Common helper to build BehaviourOccurrences
    fun buildOccurrences(
        behaviours: List<UnsafeBehaviourModel>,
        mostFrequentType: String?,
        locationIdToRoadName: Map<UUID, String>
    ): List<BehaviourOccurrence> {
        return behaviours.filter { it.behaviorType == mostFrequentType }.map { behavior ->
            val dateTime = Instant.ofEpochMilli(behavior.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
            val date = dateTime.toLocalDate()
            val time = dateTime.toLocalTime().truncatedTo(ChronoUnit.MINUTES)
            val roadName = behavior.locationId?.let { locationIdToRoadName[it] } ?: "Unknown Road"
            BehaviourOccurrence(date = date, time = time, roadName = roadName)
        }
    }

    // Handle LAST_TRIP scenario
    if (periodType == PeriodType.LAST_TRIP) {
        val lastInserted = lastInsertedUnsafeBehaviour.execute() ?: return null
        val lastTrip = tripRepository.getTripById(lastInserted.tripId) ?: return null
        val lastTripId = lastTrip.id

        // Filter unsafe behaviours for the last trip
        val unsafeBehavioursLastTrip = unsafeBehaviours.filter { it.tripId == lastTripId }
        val totalIncidences = unsafeBehavioursLastTrip.size
        val (mostFrequentType, mostFrequentCount) = computeMostFrequentBehaviourInfo(unsafeBehavioursLastTrip)

        // Get all relevant location IDs
        val relevantLocationIds = buildSet {
            unsafeBehavioursLastTrip.mapNotNullTo(this) { it.locationId }
        }

        // Fetch locations once
        val locations = locationRepository.getLocationsByIds(relevantLocationIds.toList())
        val locationMap = locations.associateBy { it.id }

        // If we have location data, compute distance, speed and names
        if (locationMap.isNotEmpty()) {
            val sortedLocations = locationMap.values.sortedBy { it.timestamp }
            val (totalDistanceKm, averageSpeed) = computeDistanceAndAverageSpeed(sortedLocations)

            val startLocationData = sortedLocations.first()
            val endLocationData = sortedLocations.last()

            val extendedLocationIds = buildSet {
                addAll(relevantLocationIds)
                add(startLocationData.id)
                add(endLocationData.id)
            }

            // locationMap already contains all these, no need to refetch
            val locationIdToRoadName = getRoadNamesForLocations(context, locationMap, osmApiService)

            val locationIdToRoadNameNonNull = locationIdToRoadName.mapValues { it.value ?: "Unknown Road" }
            val occurrences = buildOccurrences(unsafeBehavioursLastTrip, mostFrequentType, locationIdToRoadNameNonNull)


            val tripDuration = Duration.between(
                Instant.ofEpochMilli(lastTrip.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                Instant.ofEpochMilli(lastTrip.endTime!!).atZone(ZoneId.systemDefault()).toLocalDateTime()
            )

            val startTime = Instant.ofEpochMilli(lastTrip.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
            val endTime = Instant.ofEpochMilli(lastTrip.endTime!!).atZone(ZoneId.systemDefault()).toLocalDateTime()

            val startLocationName = locationIdToRoadName[startLocationData.id] ?: "Unknown Location"
            val endLocationName = locationIdToRoadName[endLocationData.id] ?: "Unknown Location"

            val reportingPeriodToday = PeriodUtils.getReportingPeriod(PeriodType.TODAY)
            val createdDate = reportingPeriodToday?.first ?: java.time.LocalDate.now()

            return ReportStatistics(
                id = UUID.randomUUID(),
                driverProfileId = driverProfileId,
                tripId = lastTrip.id,
                startDate = startDate,
                endDate = endDate,
                createdDate = createdDate,
                totalIncidences = totalIncidences,
                mostFrequentUnsafeBehaviour = mostFrequentType,
                mostFrequentBehaviourCount = mostFrequentCount,
                mostFrequentBehaviourOccurrences = occurrences,
                lastTripDuration = tripDuration,
                lastTripDistance = totalDistanceKm,
                lastTripAverageSpeed = averageSpeed,
                lastTripStartLocation = startLocationName,
                lastTripEndLocation = endLocationName,
                lastTripStartTime = startTime,
                lastTripEndTime = endTime,
                lastTripInfluence = lastTrip.influence
            )
        } else {
            // No location data available, still build occurrences with "Unknown Road"
            val occurrences = buildOccurrences(unsafeBehavioursLastTrip, mostFrequentType, emptyMap())

            val tripDuration = Duration.between(
                Instant.ofEpochMilli(lastTrip.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                Instant.ofEpochMilli(lastTrip.endTime!!).atZone(ZoneId.systemDefault()).toLocalDateTime()
            )

            val startTime = Instant.ofEpochMilli(lastTrip.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
            val endTime = Instant.ofEpochMilli(lastTrip.endTime!!).atZone(ZoneId.systemDefault()).toLocalDateTime()

            val reportingPeriodToday = PeriodUtils.getReportingPeriod(PeriodType.TODAY)
            val createdDate = reportingPeriodToday?.first ?: java.time.LocalDate.now()

            return ReportStatistics(
                id = UUID.randomUUID(),
                driverProfileId = driverProfileId,
                tripId = lastTrip.id,
                startDate = startDate,
                endDate = endDate,
                createdDate = createdDate,
                totalIncidences = totalIncidences,
                mostFrequentUnsafeBehaviour = mostFrequentType,
                mostFrequentBehaviourCount = mostFrequentCount,
                mostFrequentBehaviourOccurrences = occurrences,
                lastTripDuration = tripDuration,
                lastTripInfluence = lastTrip.influence,
                // Distance, speed, start/end location omitted due to no location data
                lastTripDistance = null,
                lastTripAverageSpeed = null,
                lastTripStartLocation = null,
                lastTripEndLocation = null,
                lastTripStartTime = startTime,
                lastTripEndTime = endTime
            )
        }

    } else {
        // Handling other PeriodTypes
        val uniqueLocationIds = unsafeBehaviours.mapNotNull { it.locationId }.distinct()
        val locations = locationRepository.getLocationsByIds(uniqueLocationIds)
        val locationMap = locations.associateBy { it.id }
        val locationIdToRoadName = getRoadNamesForLocations(context, locationMap, osmApiService)

        val (mostFrequentType, mostFrequentCount) = computeMostFrequentBehaviourInfo(unsafeBehaviours)
        val locationIdToRoadNameNonNull = locationIdToRoadName.mapValues { it.value ?: "Unknown Road" }
        val occurrences = buildOccurrences(unsafeBehaviours, mostFrequentType, locationIdToRoadNameNonNull)

        // Compute reporting periods once
        val reportingPeriodToday = PeriodUtils.getReportingPeriod(PeriodType.TODAY)
        val reportingPeriod = PeriodUtils.getReportingPeriod(periodType)

        val computedStartDate = reportingPeriod?.first ?: LocalDate.now()
        val computedEndDate = reportingPeriod?.second ?: LocalDate.now()
        val createdDate = reportingPeriodToday?.first ?: LocalDate.now()

        val tripsInPeriod = tripRepository.getTripsBetweenDates(computedStartDate, computedEndDate)
        val numberOfTrips = tripsInPeriod.size

        val tripsWithIncidences = tripsInPeriod.filter { trip ->
            unsafeBehaviours.any { it.tripId == trip.id }
        }
        val numberOfTripsWithIncidences = tripsWithIncidences.size

        val tripsWithAlcoholInfluence = tripsInPeriod.filter { it.influence == "alcohol" }
        val numberOfTripsWithAlcoholInfluence = tripsWithAlcoholInfluence.size

        val totalIncidences = unsafeBehaviours.size
        val incidencesPerTrip = unsafeBehaviours.groupBy { it.tripId }.mapValues { it.value.size }

        val tripWithMostIncidencesId = incidencesPerTrip.maxByOrNull { it.value }?.key
        val tripWithMostIncidences = tripsInPeriod.find { it.id == tripWithMostIncidencesId }

        val periodDurationDays = ChronoUnit.DAYS.between(computedStartDate, computedEndDate) + 1
        val aggregationLevel = getAggregationLevel(periodDurationDays)

        // Aggregation functions
        fun aggregateByLevel(date: LocalDate): LocalDate = when (aggregationLevel) {
            AggregationLevel.DAILY -> date
            AggregationLevel.WEEKLY -> date.with(WeekFields.ISO.dayOfWeek(), 1)
            AggregationLevel.MONTHLY -> date.withDayOfMonth(1)
        }

        val tripsPerAggregationUnit = tripsInPeriod.groupBy { aggregateByLevel(
            Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
        ) }.mapValues { it.value.size }

        val incidencesPerAggregationUnit = unsafeBehaviours.groupBy {
            val date = Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            aggregateByLevel(date)
        }.mapValues { it.value.size }

        val aggregationUnitWithMostIncidences = incidencesPerAggregationUnit.maxByOrNull { it.value }?.key

        return ReportStatistics(
            id = UUID.randomUUID(),
            driverProfileId = driverProfileId,
            startDate = computedStartDate,
            endDate = computedEndDate,
            createdDate = createdDate,
            totalIncidences = totalIncidences,
            mostFrequentUnsafeBehaviour = mostFrequentType,
            mostFrequentBehaviourCount = mostFrequentCount,
            mostFrequentBehaviourOccurrences = occurrences,
            numberOfTrips = numberOfTrips,
            incidencesPerTrip = incidencesPerTrip,
            numberOfTripsWithIncidences = numberOfTripsWithIncidences,
            numberOfTripsWithAlcoholInfluence = numberOfTripsWithAlcoholInfluence,
            tripWithMostIncidences = tripWithMostIncidences,
            aggregationLevel = aggregationLevel,
            tripsPerAggregationUnit = tripsPerAggregationUnit,
            incidencesPerAggregationUnit = incidencesPerAggregationUnit,
            aggregationUnitWithMostIncidences = aggregationUnitWithMostIncidences
        )
    }
}


private fun getLocalDateTimeFromTrip(startTime: Any): LocalDateTime {
    return when (startTime) {
        is Long -> Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
        is Instant -> startTime.atZone(ZoneId.systemDefault()).toLocalDateTime()
        is LocalDateTime -> startTime
        else -> throw IllegalArgumentException("Unsupported startTime type")
    }
}
private fun getLocalDateTimeFromBehavior(behavior: UnsafeBehaviourModel): LocalDateTime {
    return Instant.ofEpochMilli(behavior.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
}

private fun computeDistanceAndAverageSpeed(locations: List<LocationData>): Pair<Double, Double> {
    var totalDistance = 0.0 // in kilometers
    var totalTimeSeconds = 0.0

    for (i in 0 until locations.size - 1) {
        val startLocation = locations[i]
        val endLocation = locations[i + 1]

        // Compute distance between two locations
        val distance = haversineDistance(
            startLocation.latitude,
            startLocation.longitude,
            endLocation.latitude,
            endLocation.longitude
        )
        totalDistance += distance

        // Compute time difference between two locations
        val timeDiff = (endLocation.timestamp - startLocation.timestamp) / 1000.0 // Convert ms to seconds
        totalTimeSeconds += timeDiff
    }

    // Compute average speed in km/h
    val averageSpeed = if (totalTimeSeconds > 0) {
        (totalDistance / (totalTimeSeconds / 3600.0)) // totalTimeSeconds converted to hours
    } else {
        0.0
    }

    return Pair(totalDistance, averageSpeed)
}

private fun haversineDistance(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val R = 6371.0 // Earth's radius in kilometers
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2.0)
    val c = 2 * asin(sqrt(a))
    return R * c
}

private suspend fun getRoadNamesForLocations(
    context: Context,
    locationMap: Map<UUID, LocationData>,
    osmApiService: OSMApiService
): Map<UUID, String?> = coroutineScope {
    val roadNameCache = mutableMapOf<Pair<Double, Double>, String?>()
    val semaphore = Semaphore(1) // Adjust according to OSM rate limits
    val uniqueCoordinates = locationMap.values
        .map { Pair(it.latitude, it.longitude) }
        .distinct()

    // Launch a coroutine for each coordinate to get road names
    uniqueCoordinates.map { coordinate ->
        async {
            semaphore.withPermit {
                try {
                    // Attempt to get road name from OSM API service
                    val roadName = getRoadNameFromOSM(osmApiService, coordinate.first, coordinate.second)
                    roadNameCache[coordinate] = roadName
                } catch (e: Exception) {
                    // Handle OSM failure by trying to get road name internally
                    try {
                        val fallbackRoadName = getRoadNameInternally(context, coordinate.first, coordinate.second)
                        roadNameCache[coordinate] = fallbackRoadName
                    } catch (e: Exception) {
                        // If both attempts fail, store null
                        roadNameCache[coordinate] = null
                    }
                }
            }
        }
    }.awaitAll()

    // Map the original UUIDs to the corresponding road names
    locationMap.mapValues { (_, location) ->
        val coordinate = Pair(location.latitude, location.longitude)
        roadNameCache[coordinate]
    }
}


private suspend fun getRoadNameFromOSM(osmApiService: OSMApiService,latitude: Double, longitude: Double): String? {
    return try {
        val response = osmApiService.getReverseGeocoding(
            format = "json",
            lat = latitude,
            lon = longitude,
            zoom = 18,
            addressdetails = 1
        )
        response.address.getAddressLine(1) // Assuming displayName contains the road name


    } catch (e: HttpException) {
        Log.e("NLGEngineViewModel", "HTTP error: ${e.code()} - ${e.message()}")
        null
    } catch (e: Exception) {
        Log.e("NLGEngineViewModel", "Error fetching road name from OSM", e)
        null
    }
}

private fun getRoadNameInternally(context: Context, latitude: Double, longitude: Double): String? {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses = geocoder.getFromLocation(latitude, longitude, 1)

    if (addresses != null && addresses.isNotEmpty()) {
        val address = addresses[0]
        val addressLine = address.getAddressLine(0)
        // Return the human-readable address line
        return addressLine
    }

    return null
}
