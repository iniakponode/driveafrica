package com.uoa.core.utils

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.RoadRepository
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.model.ReportStatistics
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.model.LocationData
import com.uoa.core.network.apiservices.OSMRoadApiService
import com.uoa.core.model.AggregationLevel
import com.uoa.core.model.BehaviourOccurrence
import com.uoa.core.model.Road
import com.uoa.core.model.getAggregationLevel
import com.uoa.core.network.apiservices.OSMSpeedLimitApiService
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import retrofit2.HttpException
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.*
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID
import kotlin.collections.get
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

suspend fun computeReportStatistics(
    context: Context,
    osmRoadApiService: OSMRoadApiService,
    osmSpeedLimitApiService: OSMSpeedLimitApiService,
    roadRepository: RoadRepository,
    startDate: LocalDate,
    endDate: LocalDate,
    periodType: PeriodType,
    unsafeBehaviours: List<UnsafeBehaviourModel>,
    tripRepository: TripDataRepository,
    locationRepository: LocationRepository,
    lastInsertedUnsafeBehaviour: com.uoa.core.utils.GetLastInsertedUnsafeBehaviourUseCase
): ReportStatistics? {
    // For non-LAST_TRIP, if no unsafe behaviours exist, return null.
    if (unsafeBehaviours.isEmpty() && periodType != PeriodType.LAST_TRIP) {
        Log.w("computeReportStatistics", "No unsafe behaviours available for periodType: $periodType")
        return null
    }

    // Retrieve driver profile ID from shared preferences.
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val profileIdString = prefs.getString(DRIVER_PROFILE_ID, null)
    if (profileIdString.isNullOrEmpty()) {
        Log.e("computeReportStatistics", "Driver profile ID missing in preferences.")
        return null
    }
    val driverProfileId = UUID.fromString(profileIdString)

    // Helper: Compute the most frequent behaviour and its count.
    fun computeMostFrequentBehaviourInfo(behaviours: List<UnsafeBehaviourModel>): Pair<String?, Int> {
        val behaviourCounts = behaviours.groupingBy { it.behaviorType }.eachCount()
        val maxEntry = behaviourCounts.maxByOrNull { it.value }
        return maxEntry?.toPair() ?: (null to 0)
    }

    // Helper: Build a list of occurrences for the given behaviour type.
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

    // ------------------- LAST_TRIP branch -------------------
    if (periodType == PeriodType.LAST_TRIP) {
        val lastInserted = lastInsertedUnsafeBehaviour.execute()
        if (lastInserted == null) {
            Log.e("computeReportStatistics", "No last inserted unsafe behaviour found.")
            return null
        }
        val lastTrip = tripRepository.getTripById(lastInserted.tripId)
        if (lastTrip == null) {
            Log.e("computeReportStatistics", "No trip found for unsafe behaviour: ${lastInserted.tripId}")
            return null
        }
        // Check critical trip data
        val tripEndTime = lastTrip.endTime
        if (tripEndTime == null) {
            Log.e("computeReportStatistics", "Trip end time is null for trip: ${lastTrip.id}")
            return null
        }
        val lastTripId = lastTrip.id

        // Filter unsafe behaviours to those from the last trip.
        val unsafeBehavioursLastTrip = unsafeBehaviours.filter { it.tripId == lastTripId }
        val totalIncidences = unsafeBehavioursLastTrip.size
        val (mostFrequentType, mostFrequentCount) = computeMostFrequentBehaviourInfo(unsafeBehavioursLastTrip)

        // Gather all location IDs from behaviours.
        val relevantLocationIds = unsafeBehavioursLastTrip.mapNotNull { it.locationId }.toSet()
        val locations = locationRepository.getLocationsByIds(relevantLocationIds.toList())
        val locationMap = locations.associateBy { it.id }

        // If we have location data, process further.
        if (locationMap.isNotEmpty()) {
            val sortedLocations = locationMap.values.sortedBy { it.timestamp }
            // Compute distance and average speed safely.
            val (totalDistanceKm, averageSpeed) = computeDistanceAndAverageSpeed(sortedLocations)
            val startLocationData = sortedLocations.firstOrNull()
            val endLocationData = sortedLocations.lastOrNull()

            // Use fallback values if start or end location is missing.
            if (startLocationData == null || endLocationData == null) {
                Log.w("computeReportStatistics", "Incomplete location data for last trip.")
            }

            // Get road data (with caching and fallbacks inside getRoadDataForLocations).
            val roadDataMap = getRoadDataForLocations(
                context,
                locationMap,
                osmRoadApiService,
                osmSpeedLimitApiService,
                roadRepository,
                driverProfileId
            )
            // Map location IDs to non-null road names.
            val locationIdToRoadNameNonNull = roadDataMap.mapValues { it.value.first ?: "Unknown Road" }
            val occurrences = buildOccurrences(unsafeBehavioursLastTrip, mostFrequentType, locationIdToRoadNameNonNull)

            val tripDuration = Duration.between(
                Instant.ofEpochMilli(lastTrip.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                Instant.ofEpochMilli(tripEndTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
            )
            val startTime = Instant.ofEpochMilli(lastTrip.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
            val endTime = Instant.ofEpochMilli(tripEndTime).atZone(ZoneId.systemDefault()).toLocalDateTime()

            val startLocationName = roadDataMap[startLocationData?.id]?.first ?: "Unknown Location"
            val endLocationName = roadDataMap[endLocationData?.id]?.first ?: "Unknown Location"

            val reportingPeriodToday = PeriodUtils.getReportingPeriod(PeriodType.TODAY)
            val createdDate = reportingPeriodToday?.first ?: LocalDate.now()

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
            Log.w("computeReportStatistics", "No location data available for last trip; using fallbacks.")
            val occurrences = buildOccurrences(unsafeBehavioursLastTrip, mostFrequentType, emptyMap())
            val tripDuration = Duration.between(
                Instant.ofEpochMilli(lastTrip.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                Instant.ofEpochMilli(tripEndTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
            )
            val startTime = Instant.ofEpochMilli(lastTrip.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
            val endTime = Instant.ofEpochMilli(tripEndTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
            val reportingPeriodToday = PeriodUtils.getReportingPeriod(PeriodType.TODAY)
            val createdDate = reportingPeriodToday?.first ?: LocalDate.now()
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
                lastTripDistance = null,
                lastTripAverageSpeed = null,
                lastTripStartLocation = null,
                lastTripEndLocation = null,
                lastTripStartTime = startTime,
                lastTripEndTime = endTime
            )
        }
    }
    // ------------------- Non-LAST_TRIP branch -------------------
    else {
        // Get unique location IDs and fetch locations.
        val uniqueLocationIds = unsafeBehaviours.mapNotNull { it.locationId }.distinct()
        val locations = locationRepository.getLocationsByIds(uniqueLocationIds)
        val locationMap = locations.associateBy { it.id }
        // Get road data for these locations.
        val roadDataMap = getRoadDataForLocations(
            context,
            locationMap,
            osmRoadApiService,
            osmSpeedLimitApiService,
            roadRepository,
            driverProfileId
        )
        val (mostFrequentType, mostFrequentCount) = computeMostFrequentBehaviourInfo(unsafeBehaviours)
        val locationIdToRoadNameNonNull = roadDataMap.mapValues { it.value.first ?: "Unknown Road" }
        val occurrences = buildOccurrences(unsafeBehaviours, mostFrequentType, locationIdToRoadNameNonNull)

        // Determine reporting periods.
        val reportingPeriodToday = PeriodUtils.getReportingPeriod(PeriodType.TODAY)
        val reportingPeriod = PeriodUtils.getReportingPeriod(periodType)
        val computedStartDate = reportingPeriod?.first ?: LocalDate.now()
        val computedEndDate = reportingPeriod?.second ?: LocalDate.now()
        val createdDate = reportingPeriodToday?.first ?: LocalDate.now()

        // Get trips in this period.
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

private suspend fun getRoadDataForLocations(
    context: Context,
    locationMap: Map<UUID, LocationData>,
    osmApiService: OSMRoadApiService,
    speedLimitApiService: OSMSpeedLimitApiService,
    roadRepository: RoadRepository,
    profileId: UUID
): Map<UUID, Pair<String?, Int?>> = coroutineScope {

    // Caches for road names and speed limits
    val roadNameCache = mutableMapOf<Pair<Double, Double>, String?>()
    val speedLimitCache = mutableMapOf<Pair<Double, Double>, Int?>()

    // Semaphore to enforce a single request at a time (rate limiting)
    val semaphore = Semaphore(1)

    // Deduplicate coordinates to reduce repeated requests
    val uniqueCoordinates = locationMap.values
        .map { Pair(it.latitude, it.longitude) }
        .distinct()

    // For each unique coordinate, concurrently fetch road name and speed limit
    uniqueCoordinates.map { coordinate ->
        async {
            semaphore.withPermit {
                delay(1000) // Rate limiting: 1 request per second

                try {
                    // Fetch road name from OSM Nominatim
                    val roadName =
                        getRoadNameFromOSM(osmApiService, coordinate.first, coordinate.second)

                    // Initialize speedLimit with null
                    var speedLimit: Int? = null
                    val radius = 200.0

                    // If road name was successfully retrieved, attempt to fetch speed limit
                    // If road name was successfully retrieved, attempt to fetch speed limit
                    if (roadName != null) {
                        // Build Overpass query for speed limit
                        val query = buildSpeedLimitQuery(
                            coordinate.first,
                            coordinate.second,
                            radius = radius
                        )
                        val response = speedLimitApiService.fetchSpeedLimits(query)

                        // Extract maxspeed from Overpass response (simple numeric parsing)
                        val speedLimit = response.elements.firstOrNull()
                            ?.tags?.get("maxspeed")
                            ?.filter { it.isDigit() }
                            ?.toIntOrNull() ?: 0

                        val roadType = response.elements.firstOrNull()
                            ?.tags?.get("highway") ?: "unknown"

                        // Create a Road object with the fetched data
                        val road = Road(
                            id = UUID.randomUUID(),
                            driverProfileId = profileId, // Ensure profileId is defined in context
                            name = roadName,
                            roadType = roadType,
                            speedLimit = speedLimit,
                            latitude = coordinate.first,
                            longitude = coordinate.second,
                            radius = radius,
                            sync = false
                        )

                        // Save remotely first, then locally
//                        val saveResult = roadRepository.saveOrUpdateRoadRemotelyFirst(road)
                        val savedLocalResult = roadRepository.saveRoadLocally(road)
                        if (savedLocalResult is Resource.Success) {
                            Log.d("RoadSave", "Road saved remotely and locally successfully")
                        } else if (savedLocalResult is Resource.Error) {
                            Log.e("RoadSave", "Error saving road: ${savedLocalResult.message}")
                        }
                    }


                    // Cache the fetched values
                    roadNameCache[coordinate] = roadName
                    speedLimitCache[coordinate] = speedLimit

                } catch (e: Exception) {
                    Log.e("NLGEngine", "Error fetching road data for coordinate $coordinate", e)
                    // On error, attempt fallback for road name if needed
                    roadNameCache[coordinate] = try {
                        getRoadNameInternally(context, coordinate.first, coordinate.second)
                    } catch (fallbackE: Exception) {
                        Log.e("NLGEngine", "Fallback geocoder failed for road name", fallbackE)
                        null
                    }
                    speedLimitCache[coordinate] = null
                }
            }
        }
    }.awaitAll()

    // Map each UUID to a pair of road name and speed limit from the caches
    locationMap.mapValues { (_, locationData) ->
        val coordinate = Pair(locationData.latitude, locationData.longitude)
        Pair(roadNameCache[coordinate], speedLimitCache[coordinate])
    }
}

/**
 * Fetches road data (road name and speed limit) for a single location.
 *
 * This function uses OSM reverse geocoding to get the road name and the Overpass API
 * to fetch the speed limit. It applies a global rate limit before making requests,
 * saves a Road object locally (using [roadRepository]), and handles failures by
 * attempting a fallback via the internal geocoder.
 *
 * @param context Application context.
 * @param location The [LocationData] instance for which to fetch road data.
 * @param osmApiService The OSM road API service.
 * @param speedLimitApiService The OSM speed limit API service.
 * @param roadRepository Repository for saving road information.
 * @param profileId The driver's profile ID.
 *
 * @return A [Pair] where the first element is the road name (nullable) and the second element is the speed limit (nullable).
 */
// Cache to store the last coordinate processed and its corresponding result.
private var lastRoadDataCoordinate: Pair<Double, Double>? = null
private var lastRoadDataResult: Pair<String?, Int?>? = null

suspend fun getRoadDataForLocation(
    context: Context,
    location: LocationData,
    osmApiService: OSMRoadApiService,
    speedLimitApiService: OSMSpeedLimitApiService,
    roadRepository: RoadRepository,
    profileId: UUID
): Pair<String?, Int?> = coroutineScope {
    // Build the coordinate pair from location data.
    val coordinate = Pair(location.latitude, location.longitude)

    // Check if the current coordinate is the same as the last one.
    if (lastRoadDataCoordinate != null && lastRoadDataCoordinate == coordinate) {
        lastRoadDataResult?.let {
            Log.d("getRoadDataForLocation", "Returning cached result for coordinate: $coordinate")
            return@coroutineScope it
        }
    }

    try {
        // Apply global rate limiting (ensures at least one second between requests).
        applyRateLimit()

        // Fetch the road name using OSM reverse geocoding.
        val roadName = getRoadNameFromOSM(osmApiService, location.latitude, location.longitude)

        var speedLimit: Int? = null

        if (roadName != null) {
            val radius = 200.0
            // Build Overpass API query to fetch speed limit information.
            val query = buildSpeedLimitQuery(location.latitude, location.longitude, radius)
            val response = speedLimitApiService.fetchSpeedLimits(query)

            // Extract the speed limit from the response.
            speedLimit = response.elements.firstOrNull()
                ?.tags?.get("maxspeed")
                ?.filter { it.isDigit() }
                ?.toIntOrNull() ?: 0

            // Optionally, retrieve the road type (defaulting to "unknown").
            val roadType = response.elements.firstOrNull()?.tags?.get("highway") ?: "unknown"

            // Build a Road object with the fetched data.
            val road = Road(
                id = UUID.randomUUID(),
                driverProfileId = profileId,
                name = roadName,
                roadType = roadType,
                speedLimit = speedLimit,
                latitude = location.latitude,
                longitude = location.longitude,
                radius = radius,
                sync = false
            )

            // Save the Road object locally.
            val savedLocalResult = roadRepository.saveRoadLocally(road)
            when (savedLocalResult) {
                is Resource.Success -> {
                    Log.d("RoadSave", "Road saved successfully for coordinate: $coordinate")
                }
                is Resource.Error -> {
                    Log.e("RoadSave", "Error saving road: ${savedLocalResult.message}")
                }
                Resource.Loading -> Log.d("RoadSave", "Fetching Road Name")
            }
        }
        // Build the result.
        val result = Pair(roadName, speedLimit)
        // Cache the coordinate and its corresponding result.
        lastRoadDataCoordinate = coordinate
        lastRoadDataResult = result

        result
    } catch (e: Exception) {
        Log.e("getRoadDataForLocation", "Error fetching road data for coordinate $coordinate", e)
        // On error, attempt fallback for road name using an internal geocoder.
        val fallbackRoadName = try {
            getRoadNameInternally(context, location.latitude, location.longitude)
        } catch (fallbackE: Exception) {
            Log.e("getRoadDataForLocation", "Fallback geocoder failed for road name", fallbackE)
            null
        }
        Pair(fallbackRoadName, null)
    }
}




private suspend fun getSpeedLimitsForLocations(
    context: Context,
    locationMap: Map<UUID, LocationData>,
    speedLimitApiService: OSMSpeedLimitApiService
): Map<UUID, Int?> = coroutineScope {

    val speedLimitCache = mutableMapOf<Pair<Double, Double>, Int?>()
    val semaphore = Semaphore(1)
    val uniqueCoordinates = locationMap.values
        .map { Pair(it.latitude, it.longitude) }
        .distinct()

    uniqueCoordinates.map { coordinate ->
        async {
            semaphore.withPermit {
                // Wait 1 second to comply with Overpass API rate limits
                delay(1000)

                try {
                    // Build query for current coordinate
                    val query =
                        buildSpeedLimitQuery(coordinate.first, coordinate.second, radius = 200.0)
                    val response = speedLimitApiService.fetchSpeedLimits(query)

                    // Extract the first found speed limit for this coordinate
                    val maxSpeed = response.elements.firstOrNull()?.tags?.get("maxspeed")
                    val speedLimit = maxSpeed?.filter { it.isDigit() }?.toIntOrNull()
                    speedLimitCache[coordinate] = speedLimit
                } catch (e: Exception) {
                    Log.e("SpeedLimitService", "Error fetching speed limit from Overpass", e)
                    speedLimitCache[coordinate] = null
                }
            }
        }
    }.awaitAll()

    // Map UUIDs to fetched speed limits
    locationMap.mapValues { (_, locationData) ->
        val coordinate = Pair(locationData.latitude, locationData.longitude)
        speedLimitCache[coordinate]
    }
}


// Mutex and timestamp to enforce a global rate limit (1 request/second)
private val rateLimitMutex = Mutex()
private var lastCallTimestamp = 0L

// This suspending function will wait if needed to ensure 1 second has passed since the last call.
private suspend fun applyRateLimit() {
    rateLimitMutex.withLock {
        val now = System.currentTimeMillis()
        val elapsed = now - lastCallTimestamp
        val waitTime = 1000 - elapsed
        if (waitTime > 0) {
            delay(waitTime)
        }
        lastCallTimestamp = System.currentTimeMillis()
    }
}

private suspend fun getRoadNameFromOSM(
    osmRoadApiService: OSMRoadApiService,
    latitude: Double,
    longitude: Double
): String {
    return try {
        applyRateLimit()
        val response = osmRoadApiService.reverseGeocode(
            lat = latitude,
            lon = longitude,
            format = "jsonv2"
        )
        // Use the "road" field if available, otherwise fall back to displayName;
        // if both are missing, return a safe fallback.
        response.address?.road ?: response.displayName ?: "Unknown Road"
    } catch (e: HttpException) {
        Log.e("NLGEngine", "HTTP error: ${e.code()} - ${e.message()}")
        "Unknown Road"
    } catch (e: Exception) {
        Log.e("NLGEngine", "Error fetching road name from OSM", e)
        "Unknown Road"
    }
}


private fun getRoadNameInternally(context: Context, latitude: Double, longitude: Double): String? {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses = geocoder.getFromLocation(latitude, longitude, 1)

    // If we get a valid address, return a readable name or first address line
    return if (!addresses.isNullOrEmpty()) {
        val address = addresses[0]
        address.getAddressLine(0) // or address.thoroughfare for the road
    } else null
}


