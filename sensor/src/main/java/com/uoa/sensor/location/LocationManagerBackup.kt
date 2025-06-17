//package com.uoa.sensor.location
//
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.flow.launchIn
//import kotlinx.coroutines.flow.onEach
//import kotlinx.coroutines.launch
//import android.location.Location
//import android.os.Build
//import android.os.Looper
//import android.os.SystemClock
//import android.util.Log
//import androidx.annotation.RequiresApi
//import com.google.android.gms.location.DetectedActivity
//import com.google.android.gms.location.FusedLocationProviderClient
//import com.google.android.gms.location.LocationCallback
//import com.google.android.gms.location.LocationRequest
//import com.google.android.gms.location.LocationResult
//import com.google.android.gms.location.Priority
//import com.google.android.gms.location.LocationRequest.Builder
////import com.uoa.sensor.hardware.VehicleMovementManager
//import com.uoa.core.network.apiservices.OSMSpeedLimitApiService
//import com.uoa.core.model.LocationData
//import com.uoa.sensor.repository.SensorDataColStateRepository
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//import java.util.TimeZone
//import java.util.UUID
//import java.util.concurrent.TimeUnit
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@RequiresApi(Build.VERSION_CODES.O)
//@Singleton
//class LocationManagerBackup @Inject constructor(
//    private val bufferManager: LocationDataBufferManager,
//    private val fusedLocationProviderClient: FusedLocationProviderClient,
//    private val osmSpeedLimitApiService: OSMSpeedLimitApiService,
//    private val sensorDataColStateRepository: SensorDataColStateRepository // Injected repository
//) {
//
//    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
//    private var lastRecordedLocation: Location? = null
//
//    private val intervalMovingMillis: Long = 20 * 1000
//    private val intervalStationaryMillis: Long = 5 * 60 * 1000
//
//    private val MAX_HORIZONTAL_ACCURACY = 10f
//    private val MAX_LOCATION_AGE_MS = 2 * 60 * 1000L
//    private val MAX_VERTICAL_ACCURACY = 15f
//    private val MAX_SPEED_ACCURACY = 1f
//
//    private val locationCallback = object : LocationCallback() {
//        override fun onLocationResult(locationResult: LocationResult) {
//            locationResult.lastLocation?.let { location ->
//                if (isValidLocation(location)) {
//                    processLocation(location)
//                } else {
//                    Log.d("LocationManager", "Invalid location received")
//                }
//            }
//        }
//    }
//
//    init {
//        observeActivityRecognition()
//    }
//
//    private fun observeActivityRecognition() {
//        scope.launch {
//            sensorDataColStateRepository.currentActivity.collect { activity ->
//                if (activity?.type == DetectedActivity.IN_VEHICLE && activity.confidence >= 70) {
//                    startLocationUpdates()
//                } else {
//                    stopLocationUpdates()
//                }
//            }
//        }
//    }
//
//    fun startLocationUpdates() {
//        updateLocationRequest(isVehicleMoving = true)
//    }
//
//    private fun updateLocationRequest(isVehicleMoving: Boolean) {
//        val interval = if (isVehicleMoving) intervalMovingMillis else intervalStationaryMillis
//        val priority = if (isVehicleMoving)
//            Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
//
//        val locationRequest = LocationRequest.Builder(priority, interval)
//            .setWaitForAccurateLocation(true)
//            .setMinUpdateIntervalMillis(interval / 2)
//            .setMaxUpdateDelayMillis(interval)
//            .build()
//
//        try {
//            fusedLocationProviderClient.removeLocationUpdates(locationCallback).addOnCompleteListener {
//                try {
//                    fusedLocationProviderClient.requestLocationUpdates(
//                        locationRequest, locationCallback, Looper.getMainLooper()
//                    )
//                } catch (e: SecurityException) {
//                    Log.e("LocationManager", "Location permission not granted", e)
//                }
//            }
//        } catch (e: SecurityException) {
//            Log.e("LocationManager", "Location permission not granted", e)
//        }
//    }
//
//    fun stopLocationUpdates() {
//        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
//        bufferManager.stopBufferHandler()
//    }
//
//    private fun isValidLocation(location: Location?): Boolean {
//        if (location == null) return false
//        val locationElapsedNanos = location.elapsedRealtimeNanos
//        val currentElapsedNanos = SystemClock.elapsedRealtimeNanos()
//        if (locationElapsedNanos <= 0 || locationElapsedNanos > currentElapsedNanos) return false
//        val ageMs = TimeUnit.NANOSECONDS.toMillis(currentElapsedNanos - locationElapsedNanos)
//        return ageMs in 0..MAX_LOCATION_AGE_MS
//    }
//
//    private fun processLocation(location: Location) {
//        scope.launch {
//            val speed = location.speed
//            val distance = lastRecordedLocation?.distanceTo(location)?.toDouble() ?: 0.0
//            val query = buildSpeedLimitQuery(location.latitude, location.longitude, 200.0)
//
//            val speedLimit = try {
//                osmSpeedLimitApiService.fetchSpeedLimits(query)
//                    .elements.firstOrNull()?.tags?.get("maxspeed")
//                    ?.filter { it.isDigit() }?.toIntOrNull() ?: 0
//            } catch (e: Exception) {
//                Log.e("LocationProcessor", "Error fetching speed limits", e)
//                0
//            }
//
//            if (shouldRecordNewLocation(location)) {
//                val locationData = LocationData(
//                    id = UUID.randomUUID(),
//                    latitude = location.latitude,
//                    longitude = location.longitude,
//                    altitude = location.altitude,
//                    speed = speed.toDouble(),
//                    distance = distance,
//                    timestamp = location.time,
//                    date = Date(location.time),
//                    speedLimit = speedLimit * 0.44704,
//                    sync = false
//                )
//                lastRecordedLocation = location
//                bufferManager.addLocationData(locationData)
//            }
//        }
//    }
//
//    private fun shouldRecordNewLocation(newLocation: Location): Boolean {
//        val last = lastRecordedLocation ?: return true
//        val distance = newLocation.distanceTo(last)
//        val timeDelta = newLocation.time - last.time
//        return distance >= MAX_HORIZONTAL_ACCURACY || timeDelta >= intervalStationaryMillis
//    }
//
//    private fun buildSpeedLimitQuery(latitude: Double, longitude: Double, radius: Double): String {
//        return "query for lat:$latitude lon:$longitude radius:$radius"
//    }
//}
