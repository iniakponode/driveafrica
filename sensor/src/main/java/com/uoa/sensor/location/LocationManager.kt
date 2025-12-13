package com.uoa.sensor.location

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Handler
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
import com.uoa.core.utils.buildSpeedLimitQuery
import com.uoa.core.utils.formatDateToUTCPlusOne
import com.uoa.core.utils.getRoadDataForLocation
import com.uoa.sensor.repository.LocationRepositoryImpl
import com.uoa.sensor.repository.SensorDataColStateRepository
import com.uoa.core.utils.toEntity
import com.uoa.sensor.hardware.MotionDetection
//import com.uoa.sensor.hardware.MotionDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
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
    private val motionDetector: MotionDetection, // Injected so we can register as a listener
    private val osmSpeedLimitApiService: OSMSpeedLimitApiService,
    private val context: Context,
    private val osmRoadApiService: OSMRoadApiService,
    private val roadRepository: RoadRepository,
    private val sensorDataColStateRepository: SensorDataColStateRepository
) : MotionDetection.MotionListener {

    val driverProfileId: UUID? = PreferenceUtils.getDriverProfileId(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Keep track of the last valid location we've recorded
    private var lastRecordedLocation: Location? = null

    // This is an immediate in-memory pointer to the *latest created* location ID
    // not necessarily the one that's persisted or flushed yet.
    @Volatile
    private var latestLocationId: UUID? = null

    // Callback for forwarding location updates to DrivingStateManager
    private var externalLocationCallback: ((Location) -> Unit)? = null

    /**
     * Set a callback to receive location updates (for DrivingStateManager)
     */
    fun setLocationCallback(callback: (Location) -> Unit) {
        externalLocationCallback = callback
        Log.d("LocationManager", "External location callback registered")
    }

    // Two different intervals for location updates:
    private val intervalMovingMillis: Long = 20_000 // 20 seconds
    private val intervalStationaryMillis: Long = 5 * 60_000 // 5 minutes

    // Example thresholds
    private val MAX_HORIZONTAL_ACCURACY = 10f     // meters
    private val MAX_LOCATION_AGE_MS = 2 * 60_000L // 2 minutes

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

    init {
        // Register as a motion listener, so we can switch intervals
        motionDetector.addMotionListener(this)
    }

    // -----------------------------------------------------------------------
    //                       Public Lifecycle Methods
    // -----------------------------------------------------------------------

    /**
     * Start location updates at the "stationary" interval by default.
     * Then, if motion is detected, onMotionDetected() calls updateLocationRequest(true).
     */
    fun startLocationUpdates() {
        updateLocationRequest(isVehicleMoving = false)
    }

    /**
     * Stop location updates entirely. Also cancels background coroutines if needed.
     */
    fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        bufferManager.stopBufferHandler() // Stop the periodic location buffer flush in LocationDataBufferManager
        scope.cancel()
    }

    // -----------------------------------------------------------------------
    //               Implementing MotionListener for MotionDetector
    // -----------------------------------------------------------------------

    override fun onMotionDetected() {
        Log.d("LocationManager", "Motion detected → switching to 'moving' location interval")
        updateLocationRequest(isVehicleMoving = true)
    }

    override fun onMotionStopped() {
        Log.d("LocationManager", "Motion stopped → switching to 'stationary' location interval")
        updateLocationRequest(isVehicleMoving = false)
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
            .setWaitForAccurateLocation(true)
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
            val speed = location.speed // m/s
            val distance = lastRecordedLocation?.distanceTo(location)?.toDouble() ?: 0.0
            val clampedSpeed = speed.toDouble().takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
            sensorDataColStateRepository.updateSpeed(clampedSpeed)

            // Attempt to fetch speed limit from Overpass
            val query = buildSpeedLimitQuery(location.latitude, location.longitude, radius = 200.0)
            val response = try {
                osmSpeedLimitApiService.fetchSpeedLimits(query)
            } catch (e: Exception) {
                Log.e("LocationManager", "Error fetching speed limits", e)
                null
            }
            val speedLimit = response?.elements?.firstOrNull()
                ?.tags?.get("maxspeed")
                ?.filter { it.isDigit() }
                ?.toIntOrNull() ?: 0

            // Create and persist a Road object

//            val road = Road(
//                id = UUID.randomUUID(),
//                driverProfileId = profileId, // Update this as needed
//                name = roadName,
//                roadType = "",      // Populate if available
//                speedLimit = speedLimit ?: 0,
//                latitude = coordinate.first,
//                longitude = coordinate.second
//            )
//            roadRepository.saveOrUpdateRoad(road)

            val speedLimitMps = speedLimit * 0.44704 // Convert mph to m/s if speedLimit is in mph

            // Decide if we want to record the new location
            if (shouldRecordNewLocation(location)) {
                val locationId = UUID.randomUUID()
                val locationData = LocationData(
                    id = locationId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    speed = speed.toDouble(),
                    distance = distance,
                    timestamp = location.time,
                    date = Date(location.time),
                    speedLimit = speedLimitMps,
                    sync = false
                )

                // 1) Call our new function to fetch road data (road name + speed limit) for this location
                //    only if we have a valid driver profile ID.
                if (driverProfileId == null) {
                    Log.e(
                        "LocationManager",
                        "Driver profile ID is null or invalid; skipping road data retrieval"
                    )
                }
                val (roadName, speedLimit) = driverProfileId?.let {
                    getRoadDataForLocation(
                        context = context,
                        location = locationData,
                        osmApiService = osmRoadApiService,
                        speedLimitApiService = osmSpeedLimitApiService,
                        roadRepository = roadRepository,
                        profileId = it
                    )
                } ?: (null to null)

                // 2) Update the locationData with speed limit from Overpass (if available)
                val finalSpeedLimitMps: Double? = speedLimit?.let { it * 0.44704 } // mph→m/s if needed
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

                val roads = roadRepository.getNearByRoad(location.latitude, location.longitude, 0.05)
                sensorDataColStateRepository.updateLocation(
                    GeoPoint(location.latitude, location.longitude),
                    distance,
                    roads,
                    speedLimit ?: 0
                )
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