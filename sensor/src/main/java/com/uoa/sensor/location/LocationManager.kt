package com.uoa.sensor.location

import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.location.*
import com.uoa.core.database.repository.RoadRepository
import com.uoa.core.model.LocationData
import com.uoa.core.model.Road
import com.uoa.core.network.apiservices.OSMRoadApiService
import com.uoa.core.network.apiservices.OSMSpeedLimitApiService
import com.uoa.core.utils.PreferenceUtils
import com.uoa.core.utils.getRoadDataForLocation
import com.uoa.sensor.motion.DrivingStateManager
import com.uoa.sensor.repository.SensorDataColStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.filter
import org.osmdroid.util.GeoPoint

@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class LocationManager @Inject constructor(
    private val bufferManager: LocationDataBufferManager,
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val osmSpeedLimitApiService: OSMSpeedLimitApiService,
    private val context: Context,
    private val osmRoadApiService: OSMRoadApiService,
    private val roadRepository: RoadRepository,
    private val sensorDataColStateRepository: SensorDataColStateRepository
) {

    val driverProfileId: UUID? = PreferenceUtils.getDriverProfileId(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Keep track of the last valid location we've recorded
    private var lastRecordedLocation: Location? = null
    private var lastAcceptedLocation: Location? = null

    // This is an immediate in-memory pointer to the *latest created* location ID
    // not necessarily the one that's persisted or flushed yet.
    @Volatile
    private var latestLocationId: UUID? = null
    private var updatesEnabled = false
    private var recordingEnabled = true

    // Callback for forwarding location updates to DrivingStateManager
    private var externalLocationCallback: ((Location) -> Unit)? = null

    private data class RoadCacheEntry(
        val roadName: String?,
        val speedLimit: Int?,
        val expiresAtMs: Long
    )

    private val roadCache = object : LinkedHashMap<String, RoadCacheEntry>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, RoadCacheEntry>?): Boolean {
            return size > 256
        }
    }

    private val roadCacheHitTtlMs = 12 * 60 * 60 * 1000L
    private val roadCacheMissTtlMs = 10 * 60 * 1000L
    private val roadCacheRadius = 0.01

    private fun roadCacheKey(latitude: Double, longitude: Double): String {
        return "%s:%s".format(
            Locale.US,
            "%.3f".format(Locale.US, latitude),
            "%.3f".format(Locale.US, longitude)
        )
    }

    private fun getCachedRoadData(latitude: Double, longitude: Double): RoadCacheEntry? {
        val key = roadCacheKey(latitude, longitude)
        val now = System.currentTimeMillis()
        synchronized(roadCache) {
            val entry = roadCache[key] ?: return null
            if (now > entry.expiresAtMs) {
                roadCache.remove(key)
                return null
            }
            return entry
        }
    }

    private fun putCachedRoadData(latitude: Double, longitude: Double, roadName: String?, speedLimit: Int?) {
        val hasData = roadName != null || (speedLimit != null && speedLimit > 0)
        val ttl = if (hasData) roadCacheHitTtlMs else roadCacheMissTtlMs
        val entry = RoadCacheEntry(roadName, speedLimit, System.currentTimeMillis() + ttl)
        synchronized(roadCache) {
            roadCache[roadCacheKey(latitude, longitude)] = entry
        }
    }

    /**
     * Set a callback to receive location updates (for DrivingStateManager)
     */
    fun setLocationCallback(callback: (Location) -> Unit) {
        externalLocationCallback = callback
        Log.d("LocationManager", "External location callback registered")
    }

    // Two different intervals for location updates:
    private val intervalMovingMillis: Long = 20_000 // 20 seconds
    private val intervalStationaryMillis: Long = 60_000 // 1 minute

    // Example thresholds
    private val MAX_HORIZONTAL_ACCURACY = 10f     // meters
    private val MAX_LOCATION_AGE_MS = 2 * 60_000L // 2 minutes
    private val MAX_GPS_ACCURACY_REJECT_M = 200f
    private val MIN_SATELLITES_REQUIRED = 4
    private val MAX_PLAUSIBLE_SPEED_MPS = 55.56 // ~200 km/h
    private val MAX_GPS_ACCEL_MPS2 = 8.0
    private val MIN_SPEED_SAMPLE_DT_MS = 250L
    private val SPEED_KALMAN_PROCESS_NOISE = 1.0
    private val SPEED_KALMAN_MEASUREMENT_NOISE = 2.0
    private val speedKalmanFilter = SpeedKalmanFilter(
        processNoise = SPEED_KALMAN_PROCESS_NOISE,
        measurementNoise = SPEED_KALMAN_MEASUREMENT_NOISE
    )

    // Callback that receives raw location updates from FusedLocationProvider
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                if (isValidLocation(location)) {
                    // Forward to external callback (DrivingStateManager)
                    externalLocationCallback?.invoke(location)

                    // Process location for storage
                    processLocation(location)
                } else {
                    Log.d("LocationManager", "Invalid location received")
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    //                       Public Lifecycle Methods
    // -----------------------------------------------------------------------

    /**
     * Start location updates at the moving interval while driving state is active.
     */
    fun startLocationUpdates() {
        updatesEnabled = true
        lastAcceptedLocation = null
        speedKalmanFilter.reset()
        updateLocationRequest(isVehicleMoving = true)
    }

    /**
     * Stop location updates entirely. Also cancels background coroutines if needed.
     */
    fun stopLocationUpdates() {
        updatesEnabled = false
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    fun setRecordingEnabled(enabled: Boolean) {
        recordingEnabled = enabled
    }

    fun setDrivingState(state: DrivingStateManager.DrivingState) {
        if (!updatesEnabled) return
        val isMovingInterval = when (state) {
            DrivingStateManager.DrivingState.IDLE -> false
            DrivingStateManager.DrivingState.VERIFYING,
            DrivingStateManager.DrivingState.RECORDING -> true
            DrivingStateManager.DrivingState.POTENTIAL_STOP -> false
        }
        updateLocationRequest(isVehicleMoving = isMovingInterval)
    }
    // -----------------------------------------------------------------------
    //                        Location Request / Updates
    // -----------------------------------------------------------------------

    /**
     * Update the FusedLocationProviderClient request intervals based on whether
     * the vehicle is moving (frequent updates) or stationary (less frequent).
     */
    private fun updateLocationRequest(isVehicleMoving: Boolean) {
        val interval = if (isVehicleMoving) intervalMovingMillis else intervalStationaryMillis
        val priority = if (isVehicleMoving) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val locationRequest = LocationRequest.Builder(priority, interval)
            .setWaitForAccurateLocation(isVehicleMoving)
            .setMinUpdateIntervalMillis(interval / 2)
            .setMaxUpdateDelayMillis(interval)
            .build()

        try {
            // Remove any existing requests before requesting a new one
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                .addOnCompleteListener {
                    try {
                        fusedLocationProviderClient.requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            Looper.getMainLooper()
                        )
                    } catch (e: SecurityException) {
                        Log.e("LocationManager", "Location permission not granted", e)
                    }
                }
        } catch (e: SecurityException) {
            Log.e("LocationManager", "Location permission not granted", e)
        }
    }

    // -----------------------------------------------------------------------
    //                           Validate & Process
    // -----------------------------------------------------------------------

    /**
     * Basic validity checks for the new location fix (age, coordinates, etc.).
     */
    private fun isValidLocation(location: Location?): Boolean {
        if (location == null) return false

        // Check if location is too old
        val locationElapsedNanos = location.elapsedRealtimeNanos
        val currentElapsedNanos = SystemClock.elapsedRealtimeNanos()
        if (locationElapsedNanos <= 0 || locationElapsedNanos > currentElapsedNanos) {
            return false
        }

        val locationAgeNanos = currentElapsedNanos - locationElapsedNanos
        val locationAgeMs = TimeUnit.NANOSECONDS.toMillis(locationAgeNanos)
        if (locationAgeMs < 0 || locationAgeMs > MAX_LOCATION_AGE_MS) {
            return false
        }

        // Could also check horizontal accuracy:
        // if (!location.hasAccuracy() || location.accuracy > MAX_HORIZONTAL_ACCURACY) { ... }

        return true
    }

    /**
     * If valid, we check speed limits, build a LocationData object,
     * and insert it into LocationDataBufferManager.
     */
    private fun processLocation(location: Location) {
        scope.launch {
            val accuracy = location.takeIf { it.hasAccuracy() }?.accuracy?.toDouble()
            if (accuracy != null && accuracy > MAX_GPS_ACCURACY_REJECT_M) {
                Log.d("LocationManager", "Skipping GPS fix: accuracy=${accuracy}m")
                return@launch
            }
            val satelliteCount = getSatelliteCount(location)
            if (satelliteCount != null && satelliteCount < MIN_SATELLITES_REQUIRED) {
                Log.d("LocationManager", "Skipping GPS fix: satellites=$satelliteCount")
                return@launch
            }
            val measuredSpeed = resolveSpeedMeasurement(location, lastAcceptedLocation)
            val dtMs = lastAcceptedLocation?.let { location.time - it.time } ?: 0L
            if (isSpeedOutlier(measuredSpeed, speedKalmanFilter.currentEstimate(), dtMs)) {
                speedKalmanFilter.predict(location.time)
                Log.d(
                    "LocationManager",
                    "Skipping GPS fix: outlier speed=$measuredSpeed dtMs=$dtMs"
                )
                return@launch
            }
            val speedAccuracy = resolveSpeedAccuracy(location)
            val filteredSpeed = speedKalmanFilter.update(measuredSpeed, speedAccuracy, location.time)
            val clampedSpeed = filteredSpeed.coerceAtLeast(0.0)
            sensorDataColStateRepository.updateSpeed(clampedSpeed)
            lastAcceptedLocation = location
            if (!recordingEnabled) {
                return@launch
            }

            val distance = lastRecordedLocation?.distanceTo(location)?.toDouble() ?: 0.0
            var roadName: String? = null
            var speedLimit: Int? = null
            val cachedEntry = getCachedRoadData(location.latitude, location.longitude)
            if (cachedEntry != null) {
                roadName = cachedEntry.roadName
                speedLimit = cachedEntry.speedLimit
            } else {
                val cachedRoad = roadRepository.getRoadByCoordinates(
                    location.latitude,
                    location.longitude,
                    roadCacheRadius
                )
                if (cachedRoad != null) {
                    roadName = cachedRoad.name
                    speedLimit = cachedRoad.speedLimit
                    putCachedRoadData(location.latitude, location.longitude, roadName, speedLimit)
                }
            }

            val roads = roadRepository.getNearByRoad(location.latitude, location.longitude, 0.05)
            sensorDataColStateRepository.updateLocation(
                GeoPoint(location.latitude, location.longitude),
                distance,
                roads,
                speedLimit ?: 0
            )

            val shouldFetchRoadData =
                (roadName.isNullOrBlank() || (speedLimit ?: 0) <= 0) && cachedEntry == null
            if (shouldFetchRoadData) {
                if (driverProfileId == null) {
                    Log.e(
                        "LocationManager",
                        "Driver profile ID is null or invalid; skipping road data retrieval"
                    )
                }
                val locationDataForLookup = LocationData(
                    id = UUID.randomUUID(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    speed = clampedSpeed,
                    distance = distance,
                    accuracy = accuracy,
                    timestamp = location.time,
                    date = Date(location.time),
                    speedLimit = 0.0,
                    sync = false
                )
                val (fetchedRoadName, fetchedSpeedLimit) = driverProfileId?.let {
                    getRoadDataForLocation(
                        context = context,
                        location = locationDataForLookup,
                        osmApiService = osmRoadApiService,
                        speedLimitApiService = osmSpeedLimitApiService,
                        roadRepository = roadRepository,
                        profileId = it
                    )
                } ?: (null to null)
                if (fetchedRoadName != null || fetchedSpeedLimit != null) {
                    roadName = fetchedRoadName ?: roadName
                    speedLimit = fetchedSpeedLimit ?: speedLimit
                    putCachedRoadData(location.latitude, location.longitude, roadName, speedLimit)
                    val refreshedRoads =
                        roadRepository.getNearByRoad(location.latitude, location.longitude, 0.05)
                    sensorDataColStateRepository.updateRoadContext(
                        refreshedRoads,
                        speedLimit ?: 0
                    )
                }
            }

            // Decide if we want to record the new location
            if (shouldRecordNewLocation(location)) {
                val locationId = UUID.randomUUID()
                val speedLimitMps = speedLimit?.let { it * 0.277778 } ?: 0.0
                val locationData = LocationData(
                    id = locationId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    speed = clampedSpeed,
                    distance = distance,
                    accuracy = accuracy,
                    timestamp = location.time,
                    date = Date(location.time),
                    speedLimit = speedLimitMps,
                    sync = false
                )

                // 2) Update the locationData with speed limit from Overpass (if available)
                val finalSpeedLimitMps: Double? = speedLimit?.let { it * 0.277778 } // km/h→m/s
                val updatedLocationData = locationData.copy(
                    speedLimit = finalSpeedLimitMps?.toDouble() ?: 0.0
                )

                // 3) Log or do something with the roadName if you wish
                Log.d("LocationManager", "Fetched roadName=$roadName for locationId=$locationId")

                // Immediately update local memory with the newly-created ID
                latestLocationId = locationId
                lastRecordedLocation = location

                // Buffer the location; once inserted, the buffer manager will track currentLocationId
                bufferManager.addLocationData(updatedLocationData)
            }
        }
    }

    /**
     * Decide whether to record the new location at all.
     * Example logic: record if it's been >5 minutes or distance > threshold.
     */
    private fun shouldRecordNewLocation(newLocation: Location): Boolean {
        val oldLoc = lastRecordedLocation ?: return true // if none, record the first
        val distance = newLocation.distanceTo(oldLoc)
        val timeDelta = newLocation.time - oldLoc.time

        // E.g. if distance >= 10 meters or 5 min has passed, record
        return (distance >= MAX_HORIZONTAL_ACCURACY) || (timeDelta >= intervalStationaryMillis)
    }

    private fun getSatelliteCount(location: Location): Int? {
        val extras = location.extras ?: return null
        return when {
            extras.containsKey("satellites") -> extras.getInt("satellites")
            extras.containsKey("satellite_count") -> extras.getInt("satellite_count")
            else -> null
        }
    }

    private fun resolveSpeedMeasurement(location: Location, lastLocation: Location?): Double {
        if (location.hasSpeed()) {
            return location.speed.toDouble().coerceAtLeast(0.0)
        }
        if (lastLocation == null) return 0.0
        val dtMs = location.time - lastLocation.time
        if (dtMs <= MIN_SPEED_SAMPLE_DT_MS) return 0.0
        val distance = lastLocation.distanceTo(location).toDouble()
        return (distance / (dtMs / 1000.0)).coerceAtLeast(0.0)
    }

    private fun resolveSpeedAccuracy(location: Location): Double {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasSpeedAccuracy()) {
            location.speedAccuracyMetersPerSecond.toDouble().coerceAtLeast(0.1)
        } else {
            SPEED_KALMAN_MEASUREMENT_NOISE
        }
    }

    private fun isSpeedOutlier(measuredSpeed: Double, lastSpeed: Double?, dtMs: Long): Boolean {
        if (!measuredSpeed.isFinite()) return true
        if (measuredSpeed > MAX_PLAUSIBLE_SPEED_MPS) return true
        if (lastSpeed == null) return false
        if (dtMs <= MIN_SPEED_SAMPLE_DT_MS) return false
        val accel = kotlin.math.abs(measuredSpeed - lastSpeed) / (dtMs / 1000.0)
        return accel > MAX_GPS_ACCEL_MPS2
    }

    private class SpeedKalmanFilter(
        private val processNoise: Double,
        private val measurementNoise: Double
    ) {
        private var estimate: Double? = null
        private var variance = 1.0
        private var lastTimestampMs = 0L

        fun currentEstimate(): Double? = estimate

        fun reset() {
            estimate = null
            variance = 1.0
            lastTimestampMs = 0L
        }

        fun predict(timestampMs: Long) {
            val dtMs = (timestampMs - lastTimestampMs).coerceAtLeast(0L)
            if (estimate != null && dtMs > 0L) {
                variance += processNoise * (dtMs / 1000.0)
                lastTimestampMs = timestampMs
            }
        }

        fun update(measurement: Double, accuracy: Double?, timestampMs: Long): Double {
            if (estimate == null) {
                estimate = measurement
                lastTimestampMs = timestampMs
                variance = accuracy?.let { it * it } ?: (measurementNoise * measurementNoise)
                return measurement
            }
            val dtMs = (timestampMs - lastTimestampMs).coerceAtLeast(0L)
            if (dtMs > 0L) {
                variance += processNoise * (dtMs / 1000.0)
            }
            val r = accuracy?.let { it * it } ?: (measurementNoise * measurementNoise)
            val k = variance / (variance + r)
            estimate = estimate!! + k * (measurement - estimate!!)
            variance *= (1 - k)
            lastTimestampMs = timestampMs
            return estimate!!
        }
    }

    // -----------------------------------------------------------------------
    //                  Speed Limit Query Helper (Unchanged)
    // -----------------------------------------------------------------------

    private fun buildSpeedLimitQuery(lat: Double, lon: Double, radius: Double): String {
        // Example Overpass query building logic.  Adjust as needed.
        return """
            [out:json];
            way(around:$radius,$lat,$lon)[maxspeed];
            out;
        """.trimIndent()
    }

    // ------------------------------------------------------------
    // Expose the latest location ID
    // ------------------------------------------------------------

    /**
     * The *immediate* ID of the last created LocationData object,
     * not necessarily persisted in DB yet. (May differ from bufferManager.getCurrentLocationId()!)
     */
    fun getLatestLocationId(): UUID? {
        return latestLocationId
    }
}
