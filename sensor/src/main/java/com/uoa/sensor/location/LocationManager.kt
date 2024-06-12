package com.uoa.sensor.location

import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.location.*
import com.uoa.sensor.data.model.LocationData
import com.uoa.sensor.data.repository.LocationRepository
import com.uoa.sensor.data.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class LocationManager @Inject constructor(
    private val context: Context,
    private val locationRepository: LocationRepository,
    private val fusedLocationProviderClient: FusedLocationProviderClient
) {

    private var isCollecting: Boolean = false
    private val locationDataBuffer = mutableListOf<LocationData>()
    private val locationDataBufferLimit = 50  // Define a buffer limit
    private val handler = Handler(Looper.getMainLooper())
    private val bufferInsertInterval: Long = 5000  // Interval in milliseconds for batch insert
    private var currentLocationId: UUID? = null
    private val locationCheckIntervalMillis: Long = 20 * 60 * 1000 // 20 minutes in milliseconds
    private val locationThresholdDistanceMeters: Float = 50f // Example threshold distance in meters
    private var lastRecordedLocation: Location? = null
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.lastOrNull()?.let {
                val newLocation = Location("").apply {
                    latitude = it.latitude
                    longitude = it.longitude
                    altitude = it.altitude
                    speed = it.speed
                }

                if (shouldRecordNewLocation(newLocation)) {
                    val locationData = LocationData(
                        id =UUID.randomUUID(), // will be auto-generated
                        latitude = it.latitude.toLong(),
                        longitude = it.longitude.toLong(),
                        altitude = it.altitude,
                        speed = it.speed.toDouble(),
                        timestamp = it.time,
                        sync = false
                    )
                    lastRecordedLocation = newLocation
                    addToLocationDataBuffer(locationData)
                }
            }
        }
    }

    private fun shouldRecordNewLocation(newLocation: Location): Boolean {
        return lastRecordedLocation == null || newLocation.distanceTo(lastRecordedLocation!!) > locationThresholdDistanceMeters
    }

    init {
        startBufferInsertHandler()
    }

    fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, locationCheckIntervalMillis) // 1 hour
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(locationCheckIntervalMillis) // 1 hour
            .setMaxUpdateDelayMillis(locationCheckIntervalMillis) // 1 hour
            .build()

        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("LocationManager", "Location permission not granted", e)
        }
    }

    fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        processAndStoreLocationData()
    }

    private fun startBufferInsertHandler() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                processAndStoreLocationData()
                handler.postDelayed(this, bufferInsertInterval)
            }
        }, bufferInsertInterval)
    }

    private fun addToLocationDataBuffer(locationData: LocationData) {
        locationDataBuffer.add(locationData)
        if (locationDataBuffer.size >= locationDataBufferLimit) {
            processAndStoreLocationData()
        }
    }

    private fun processAndStoreLocationData() {
        if (locationDataBuffer.isEmpty()) return

        val bufferCopy = locationDataBuffer.toList()
        currentLocationId = bufferCopy.lastOrNull()?.id
        locationDataBuffer.clear()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                locationRepository.insertLocationBatch(bufferCopy.map { it.toEntity() }) // Save the last inserted ID
            } catch (e: Exception) {
                Log.e("LocationManager", "Error processing and storing location data", e)
            }
        }
    }
    fun getCurrentLocationId(): UUID? {
        return currentLocationId
    }
}