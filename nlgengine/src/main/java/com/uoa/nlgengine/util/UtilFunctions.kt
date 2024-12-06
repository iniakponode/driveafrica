package com.uoa.nlgengine.util

import android.app.Application
import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.nlgengine.data.model.ReportStatistics
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.model.LocationData
import com.uoa.core.network.apiservices.OSMApiService
import com.uoa.nlgengine.data.model.AggregationLevel
import com.uoa.nlgengine.data.model.BehaviourOccurrence
import com.uoa.nlgengine.data.model.getAggregationLevel
import com.uoa.nlgengine.domain.usecases.local.GetLastInsertedUnsafeBehaviourUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toLocalDateTime
import retrofit2.HttpException
import java.security.KeyStore
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.*
import java.time.temporal.WeekFields
import java.util.Date
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
    periodType: PeriodType,
    unsafeBehaviours: List<UnsafeBehaviourModel>,
    tripRepository: TripDataRepository,
    locationRepository: LocationRepository,
    lastInsertedUnsafeBehaviour: GetLastInsertedUnsafeBehaviourUseCase
): ReportStatistics? {
    if (unsafeBehaviours.isEmpty() && periodType != PeriodType.LAST_TRIP) {
        return null
    }

    // Proceed based on PeriodType
    if (periodType == PeriodType.LAST_TRIP) {

        val lastInsertedUnsafeBehaviours=lastInsertedUnsafeBehaviour.execute()


        if (lastInsertedUnsafeBehaviours != null) {
            // Retrieve all locations associated with the last trip
            // Fetch the last trip
            Log.d("Util Fun", lastInsertedUnsafeBehaviours.behaviorType)
            val lastTrip = tripRepository.getTripById(lastInsertedUnsafeBehaviours.tripId)



            // Extract unique location IDs from unsafe behaviours
            val uniqueLocationIds = unsafeBehaviours.mapNotNull { it.locationId }.distinct()

            // Get locations from repository
            val locationMap = locationRepository.getLocationsByIds(uniqueLocationIds)
                .associateBy { it.id }

            // Reverse geocode locations
//            val locationIdToRoadName = getRoadNamesForLocations(context,locationMap, osmApiService)

            Log.d("Util Fun", "Last inserted unsafe behaviour Trip id is not null")
            val lastTripId=lastTrip?.id


            if (locationMap.isNotEmpty()) {
                // Sort locations by timestamp
                val sortedLocations = locationMap.values.sortedBy { it.timestamp }

                // Compute total distance and average speed
                val (totalDistance, averageSpeed) = computeDistanceAndAverageSpeed(sortedLocations)
                val totalDistanceKm = totalDistance // Distance is already in kilometers

                // Reverse geocode start and end locations
                val startLocationData = sortedLocations.first()
                val endLocationData = sortedLocations.last()

                val uniqueLocationIds = mutableSetOf<UUID>()
                uniqueLocationIds.add(startLocationData.id)
                uniqueLocationIds.add(endLocationData.id)
                uniqueLocationIds.addAll(unsafeBehaviours.mapNotNull { it.locationId })

//                val locationMap = locationRepository.getLocationsByIds(uniqueLocationIds.toList())
//                    .associateBy { it.id }

                val locationIdToRoadName = getRoadNamesForLocations(context,locationMap, osmApiService)

                val startLocationName =
                    locationIdToRoadName[startLocationData.id] ?: "Unknown Location"
                val endLocationName = locationIdToRoadName[endLocationData.id] ?: "Unknown Location"

                // Compute total incidences in last trip
                val unsafeBehavioursLastTrip = unsafeBehaviours.filter { it.tripId == lastTripId }
                val totalIncidences = unsafeBehavioursLastTrip.size

                // Compute most frequent unsafe behaviour
                val behaviourCounts =
                    unsafeBehavioursLastTrip.groupingBy { it.behaviorType }.eachCount()
                val mostFrequentBehaviourType = behaviourCounts.maxByOrNull { it.value }?.key
                val mostFrequentBehaviourCount =
                    behaviourCounts.maxByOrNull { it.value }?.value ?: 0

                // Collect occurrences of the most frequent unsafe behaviour
                val mostFrequentBehaviours =
                    unsafeBehavioursLastTrip.filter { it.behaviorType == mostFrequentBehaviourType }
                val occurrences = mostFrequentBehaviours.map { behavior ->
                    val dateTime =
                        Instant.ofEpochMilli(behavior.timestamp).atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                    val date = dateTime.toLocalDate()
                    val time = dateTime.toLocalTime().truncatedTo(ChronoUnit.MINUTES)
                    val roadName =
                        behavior.locationId?.let { locationIdToRoadName[it] } ?: "Unknown Road"

                    BehaviourOccurrence(
                        date = date,
                        time = time,
                        roadName = roadName
                    )
                }

                // Compute trip duration
                val tripDuration = Duration.between(
                    Instant.ofEpochMilli(lastTrip!!.startTime).atZone(ZoneId.systemDefault())
                        .toLocalDateTime(),
                    Instant.ofEpochMilli(lastTrip.endTime!!).atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                )

                // Get start and end times
                val startTime =
                    Instant.ofEpochMilli(lastTrip.startTime).atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                val endTime =
                    Instant.ofEpochMilli(lastTrip.endTime!!).atZone(ZoneId.systemDefault())
                        .toLocalDateTime()

                // Alcohol influence
                val tripInfluence = lastTrip.influence // "alcohol" or "no alcohol"

                return ReportStatistics(
                    totalIncidences = totalIncidences,
                    mostFrequentUnsafeBehaviour = mostFrequentBehaviourType,
                    mostFrequentBehaviourCount = mostFrequentBehaviourCount,
                    mostFrequentBehaviourOccurrences = occurrences,
                    lastTripDuration = tripDuration.toKotlinDuration(),
                    lastTripDistance = totalDistanceKm,
                    lastTripAverageSpeed = averageSpeed,
                    lastTripStartLocation = startLocationName,
                    lastTripEndLocation = endLocationName,
                    lastTripStartTime = startTime,
                    lastTripEndTime = endTime,
                    lastTripInfluence = tripInfluence
                )

            } else {
                // No locations found for the last trip
                // Compute total incidences in last trip
                val unsafeBehavioursLastTrip = unsafeBehaviours.filter { it.tripId == lastTripId }
                val totalIncidences = unsafeBehavioursLastTrip.size

                // Compute most frequent unsafe behaviour
                val behaviourCounts =
                    unsafeBehavioursLastTrip.groupingBy { it.behaviorType }.eachCount()
                val mostFrequentBehaviourType = behaviourCounts.maxByOrNull { it.value }?.key
                val mostFrequentBehaviourCount =
                    behaviourCounts.maxByOrNull { it.value }?.value ?: 0

                // Collect occurrences of the most frequent unsafe behaviour
                val mostFrequentBehaviours =
                    unsafeBehavioursLastTrip.filter { it.behaviorType == mostFrequentBehaviourType }
                val occurrences = mostFrequentBehaviours.map { behavior ->
                    val dateTime =
                        Instant.ofEpochMilli(behavior.timestamp).atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                    val date = dateTime.toLocalDate()
                    val time = dateTime.toLocalTime().truncatedTo(ChronoUnit.MINUTES)
//                    val roadName = behavior.locationId?.let { locationIdToRoadName[it] } ?: "Unknown Road"

                    BehaviourOccurrence(
                        date = date,
                        time = time,
                        roadName = "Unknown Road"
                    )
                }

                // Compute trip duration
                val tripDuration = Duration.between(
                    Instant.ofEpochMilli(lastTrip!!.startTime).atZone(ZoneId.systemDefault())
                        .toLocalDateTime(),
                    Instant.ofEpochMilli(lastTrip.endTime!!).atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                )

                // Get start and end times
                val startTime =
                    Instant.ofEpochMilli(lastTrip.startTime).atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                val endTime =
                    Instant.ofEpochMilli(lastTrip.endTime!!).atZone(ZoneId.systemDefault())
                        .toLocalDateTime()

                // Alcohol influence
                val tripInfluence = lastTrip.influence // "alcohol" or "no alcohol"

                return ReportStatistics(
                    totalIncidences = totalIncidences,
                    mostFrequentUnsafeBehaviour = mostFrequentBehaviourType,
                    mostFrequentBehaviourCount = mostFrequentBehaviourCount,
                    mostFrequentBehaviourOccurrences = occurrences,
                    lastTripDuration = tripDuration.toKotlinDuration(),
//                    lastTripDistance = totalDistanceKm,
//                    lastTripAverageSpeed = averageSpeed,
//                    lastTripStartLocation = startLocationName,
//                    lastTripEndLocation = endLocationName,
                    lastTripStartTime = startTime,
                    lastTripEndTime = endTime,
                    lastTripInfluence = tripInfluence
                )
            }
        }
        else{
            return  null
        }
    } else {

        // Implementation for other PeriodTypes

        // Extract unique location IDs from unsafe behaviours
        val uniqueLocationIds = unsafeBehaviours.mapNotNull { it.locationId }.distinct()

        // Get locations from repository
        val locationMap = locationRepository.getLocationsByIds(uniqueLocationIds)
            .associateBy { it.id }

        // Reverse geocode locations
        val locationIdToRoadName = getRoadNamesForLocations(context,locationMap, osmApiService)

        // Compute most frequent unsafe behaviour
        val behaviourCounts = unsafeBehaviours.groupingBy { it.behaviorType }.eachCount()
        val mostFrequentBehaviourType = behaviourCounts.maxByOrNull { it.value }?.key
        val mostFrequentBehaviourCount = behaviourCounts.maxByOrNull { it.value }?.value ?: 0

        // Collect occurrences of the most frequent unsafe behaviour
        val mostFrequentBehaviours =
            unsafeBehaviours.filter { it.behaviorType == mostFrequentBehaviourType }
        val occurrences = mostFrequentBehaviours.map { behavior ->
            val dateTime = Instant.ofEpochMilli(behavior.timestamp).atZone(ZoneId.systemDefault())
                .toLocalDateTime()
            val date = dateTime.toLocalDate()
            val time = dateTime.toLocalTime().truncatedTo(ChronoUnit.MINUTES)
            val roadName = behavior.locationId?.let { locationIdToRoadName[it] } ?: "Unknown Road"

            BehaviourOccurrence(
                date = date,
                time = time,
                roadName = roadName
            )
        }

        // Declare startDate and endDate as nullable LocalDate
        var startDate: LocalDate? = null
        var endDate: LocalDate? = null


        // Determine the reporting period dates
        when (periodType) {
            PeriodType.TODAY -> {
                // Assigning value to startDate and endDate
                val today = Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                startDate = today.toKotlinLocalDate() // No need for conversion if you're using java.time.LocalDate
                endDate = today.toKotlinLocalDate()
            }

            PeriodType.THIS_WEEK -> {
                val today = Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                val startOfWeek = today.with(DayOfWeek.MONDAY)
                val endOfWeek = today.with(DayOfWeek.SUNDAY)
                Pair(startOfWeek, endOfWeek)
            }

            PeriodType.LAST_WEEK -> {

                val today = Date().toInstant().atZone(ZoneId.systemDefault())
                val startOfLastWeek = today.minusWeeks(1).with(DayOfWeek.MONDAY)
                val endOfLastWeek = today.minusWeeks(1).with(DayOfWeek.SUNDAY)
                Pair(startOfLastWeek, endOfLastWeek)
            }

//            PeriodType.CUSTOM_PERIOD -> {
//                reportPeriod ?: return null
//            }

            else -> {
                return null
            }
        }

        val today = Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        startDate = today.toKotlinLocalDate() // No need for conversion if you're using java.time.LocalDate
        endDate = today.toKotlinLocalDate()
        val tripsInPeriod =
            startDate.toJavaLocalDate().atStartOfDay(ZoneId.systemDefault())?.let {
                endDate.toJavaLocalDate().plusDays(1)?.atStartOfDay(ZoneId.systemDefault())
                    ?.let { it1 ->
                        tripRepository.getTripsBetweenDates(
                            it.toLocalDate(),
                            it1.toLocalDate()
                        )
                    }
            }

        val numberOfTrips = tripsInPeriod?.size

        val tripsWithIncidences = tripsInPeriod?.filter { trip ->
            unsafeBehaviours.any { it.tripId == trip.id }
        }
        val numberOfTripsWithIncidences = tripsWithIncidences?.size

        val tripsWithAlcoholInfluence = tripsInPeriod?.filter { it.influence == "alcohol" }
        val numberOfTripsWithAlcoholInfluence = tripsWithAlcoholInfluence?.size

        // Compute total incidences
        val totalIncidences = unsafeBehaviours.size

        // Compute trip with most incidences
        val incidencesPerTrip = unsafeBehaviours.groupBy { it.tripId }.mapValues { it.value.size }
        val tripWithMostIncidencesId = incidencesPerTrip.maxByOrNull { it.value }?.key
        val tripWithMostIncidences = tripsInPeriod?.find { it.id == tripWithMostIncidencesId }


        // Compute aggregation level based on period duration

//        startDate = today.toKotlinLocalDate() // No need for conversion if you're using java.time.LocalDate
//        endDate = today.toKotlinLocalDate()
        val periodDurationDays =
            ChronoUnit.DAYS.between(startDate.toJavaLocalDate(), endDate.toJavaLocalDate()) + 1
        val aggregationLevel = getAggregationLevel(periodDurationDays)

        // Compute trips per aggregation unit
        val tripsPerAggregationUnit = tripsInPeriod?.groupBy { trip ->
            val date =
                Instant.ofEpochMilli(trip.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
            when (aggregationLevel) {
                AggregationLevel.DAILY -> date
                AggregationLevel.WEEKLY -> date.with(WeekFields.ISO.dayOfWeek(), 1)
                AggregationLevel.MONTHLY -> date.withDayOfMonth(1)
            }
        }?.mapValues { it.value.size }

        // Compute incidences per aggregation unit
        val incidencesPerAggregationUnit = unsafeBehaviours.groupBy { behavior ->
            val date = Instant.ofEpochMilli(behavior.timestamp).atZone(ZoneId.systemDefault())
                .toLocalDate()
            when (aggregationLevel) {
                AggregationLevel.DAILY -> date
                AggregationLevel.WEEKLY -> date.with(WeekFields.ISO.dayOfWeek(), 1)
                AggregationLevel.MONTHLY -> date.withDayOfMonth(1)
            }.toKotlinLocalDate()
        }.mapValues { it.value.size }

        // Find aggregation unit with most incidences
        val aggregationUnitWithMostIncidences =
            incidencesPerAggregationUnit.maxByOrNull { it.value }?.key

        return ReportStatistics(
            totalIncidences = totalIncidences,
            mostFrequentUnsafeBehaviour = mostFrequentBehaviourType,
            mostFrequentBehaviourCount = mostFrequentBehaviourCount,
            mostFrequentBehaviourOccurrences = occurrences,
            numberOfTrips = numberOfTrips!!,
            incidencesPerTrip = incidencesPerTrip,
            numberOfTripsWithIncidences = numberOfTripsWithIncidences!!,
            numberOfTripsWithAlcoholInfluence = numberOfTripsWithAlcoholInfluence!!,
            tripWithMostIncidences = tripWithMostIncidences,
            aggregationLevel = aggregationLevel,
            tripsPerAggregationUnit = tripsPerAggregationUnit!!,
            incidencesPerAggregationUnit = incidencesPerAggregationUnit,
            aggregationUnitWithMostIncidences = aggregationUnitWithMostIncidences!!
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
