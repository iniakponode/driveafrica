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
import com.uoa.core.model.LocationData
import com.uoa.sensor.repository.LocationRepositoryImpl
import com.uoa.core.utils.toEntity
import com.uoa.sensor.hardware.MotionDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class LocationManager @Inject constructor(
    private val bufferManager: LocationDataBufferManager,
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val motionDetector: MotionDetector // Inject MotionDetector to allow listener registration
) : MotionDetector.MotionListener {

    private var lastRecordedLocation: Location? = null

    // Define location request intervals
    private val intervalMovingMillis: Long = 20 * 1000 // 20 seconds
    private val intervalStationaryMillis: Long = 5 * 60 * 1000 // 5 minutes

    // Accuracy and age thresholds
    private val MAX_HORIZONTAL_ACCURACY = 10f // meters
    private val MAX_LOCATION_AGE_MS = 2 * 60 * 1000L // 2 minutes
    private val MAX_VERTICAL_ACCURACY = 15f // meters
    private val MAX_SPEED_ACCURACY = 1f // meters per second

    // Callback to process location updates
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                // Validate the location
                if (isValidLocation(location)) {
                    processLocation(location)
                } else {
                    Log.d("LocationManager", "Invalid location received")
                }
            }
        }
    }

    init {
        // Register this LocationManager instance as a listener to the MotionDetector
        motionDetector.addMotionListener(this)
    }

    /**
     * Validate the location using recommended thresholds.
     */
    private fun isValidLocation(location: Location?): Boolean {
        // Check for null location
        if (location == null) {
            return false
        }

        // Check horizontal accuracy
//        if (!location.hasAccuracy() || location.accuracy < 0 || location.accuracy > MAX_HORIZONTAL_ACCURACY) {
//            Log.d("isValidLocation", "Invalid horizontal accuracy: ${location.accuracy}")
//            return false
//        }

        // Check if elapsedRealtimeNanos is valid
        val locationElapsedNanos = location.elapsedRealtimeNanos
        val currentElapsedNanos = SystemClock.elapsedRealtimeNanos()
        if (locationElapsedNanos <= 0 || locationElapsedNanos > currentElapsedNanos) {
            Log.d("isValidLocation", "Invalid currentElapsedNanos accuracy: ${location.accuracy}")
            return false
        }

        // Check the age of the location fix
        val locationAgeNanos = currentElapsedNanos - locationElapsedNanos
        val locationAgeMs = TimeUnit.NANOSECONDS.toMillis(locationAgeNanos)
        if (locationAgeMs < 0 || locationAgeMs > MAX_LOCATION_AGE_MS) {
            Log.d("isValidLocation", "Invalid locationAgeMs accuracy: ${location.accuracy}")
            return false
        }

//        // Optionally, check vertical accuracy if needed
//        if (location.hasVerticalAccuracy()) {
//            if (location.verticalAccuracyMeters < 0 || location.verticalAccuracyMeters > MAX_VERTICAL_ACCURACY) {
//                return false
//            }
//        }
//
//        // Optionally, check speed accuracy if needed
//        if (location.hasSpeedAccuracy()) {
//            if (location.speedAccuracyMetersPerSecond < 0 || location.speedAccuracyMetersPerSecond > MAX_SPEED_ACCURACY) {
//                return false
//            }
//        }

//        if (!location.hasAccuracy() || location.accuracy < 0 || location.accuracy > MAX_HORIZONTAL_ACCURACY) {
//            Log.d("isValidLocation", "Invalid horizontal accuracy: ${location.accuracy}")
//            return false
//        }

        return true
    }

    /**
     * Process the location and determine if it should be recorded.
     */
    private fun processLocation(location: Location) {
        val speed = location.speed  // Speed in m/s
        val distance = lastRecordedLocation?.distanceTo(location)?.toDouble() ?: 0.0

        if (shouldRecordNewLocation(location)) {
            val locationData = LocationData(
                id = UUID.randomUUID(),
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                speed = speed.toDouble(),
                distance = distance,
                timestamp = location.time,
                date = Date(location.time),
                sync = false
            )

            lastRecordedLocation = location
            bufferManager.addLocationData(locationData) // Delegate buffering to LocationDataBufferManager
        }
    }

    /**
     * Determine if the new location should be recorded.
     */
    private fun shouldRecordNewLocation(newLocation: Location): Boolean {
        // If no previous location recorded, record the new one
        if (lastRecordedLocation == null) return true

        val distance = newLocation.distanceTo(lastRecordedLocation!!)
        val timeDelta = newLocation.time - lastRecordedLocation!!.time

        return distance >= MAX_HORIZONTAL_ACCURACY || timeDelta >= intervalStationaryMillis
    }

    /**
     * Start location updates with appropriate parameters based on vehicle movement.
     */
    fun startLocationUpdates() {
        // Start with a stationary request
        updateLocationRequest(isVehicleMoving = false)
    }

    /**
     * Update the location request based on vehicle movement state.
     */
    private fun updateLocationRequest(isVehicleMoving: Boolean) {
        val interval = if (isVehicleMoving) intervalMovingMillis else intervalStationaryMillis
        val priority = if (isVehicleMoving) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY

        val locationRequest = LocationRequest.Builder(priority, interval)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(interval / 2)
            .setMaxUpdateDelayMillis(interval)
            .build()

        try {
            // Remove any existing location requests before requesting a new one
            fusedLocationProviderClient.removeLocationUpdates(locationCallback).addOnCompleteListener {
                try {
                    fusedLocationProviderClient.requestLocationUpdates(
                        locationRequest, locationCallback, Looper.getMainLooper()
                    )
                } catch (e: SecurityException) {
                    Log.e("LocationManager", "Location permission not granted", e)
                }
            }
        } catch (e: SecurityException) {
            Log.e("LocationManager", "Location permission not granted", e)
        }
    }

    /**
     * Stop location updates.
     */
    fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        bufferManager.stopBufferHandler() // Delegate stopping buffer handler to LocationDataBufferManager
    }

    // Implementing MotionListener methods from MotionDetector.MotionListener

    override fun onMotionDetected() {
        Log.d("LocationManager", "Motion detected - updating location request for moving state")
        updateLocationRequest(isVehicleMoving = true)
    }

    override fun onMotionStopped() {
        Log.d("LocationManager", "Motion stopped - updating location request for stationary state")
        updateLocationRequest(isVehicleMoving = false)
    }


}