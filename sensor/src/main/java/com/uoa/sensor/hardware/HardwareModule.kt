package com.uoa.sensor.hardware

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Priority
import com.uoa.sensor.hardware.base.TrackingSensor
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.sensor.data.model.LocationData
import com.uoa.sensor.data.model.RawSensorData
import com.uoa.sensor.data.repository.LocationRepository
import com.uoa.sensor.data.repository.RawSensorDataRepository
import com.uoa.sensor.data.toEntity
//import com.uoa.sensor.utils.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class HardwareModule @Inject constructor(
    private val context: Context,
    private val rawSensorDataRepository: RawSensorDataRepository,
    private val locationRepository: LocationRepository,
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    @AccelerometerSensorM private val accelerometerSensor: AccelerometerSensor,
    @GyroscopeSensorM private val gyroscopeSensor: GyroscopeSensor,
    @RotationVectorSensorM private val rotationVectorSensor: RotationVectorSensor,
    @MagnetometerSensorM private val magnetometerSensor: MagnetometerSensor,
    @SignificantMotionSensorM private val significantMotionSensor: SignificantMotion,
    @GravitySensorM private val gravitySensor: GravitySensor,
    @LinearAccelerationM private val linearAccelerationSensor: LinearAcceleration,
) {
    private var isCollecting: Boolean = false

    private var locationData: LocationData? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.lastOrNull()?.let {
                locationData = LocationData(id =0, it.latitude, it.longitude, it.altitude, it.speed, it.time, sync = false)
            }
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 1000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdateDelayMillis(1000)
            .build()

        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("HardwareModule", "Location permission not granted", e)
        }
    }

    fun startDataCollection(isLocationPermissionGranted: Boolean) {
        if (!isCollecting) {
            if (isLocationPermissionGranted) {
                startLocationUpdates()
            }
            accelerometerSensor.startListeningToSensor(200)
            gyroscopeSensor.startListeningToSensor(200)
            rotationVectorSensor.startListeningToSensor(200)
            magnetometerSensor.startListeningToSensor(200)
            significantMotionSensor.startListeningToSensor(200)
            gravitySensor.startListeningToSensor(200)
            linearAccelerationSensor.startListeningToSensor(200)
            isCollecting = true
        }
    }

    fun stopDataCollection() {
        if (isCollecting) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            accelerometerSensor.stopListeningToSensor()
            gyroscopeSensor.stopListeningToSensor()
            rotationVectorSensor.stopListeningToSensor()
            magnetometerSensor.stopListeningToSensor()
            significantMotionSensor.stopListeningToSensor()
            gravitySensor.stopListeningToSensor()
            isCollecting = false
        }
    }

    private fun setupListeners() {
        val sensorEventListener = { sensorType: Int, values: List<Float>, accuracy: Int ->
            val timestamp = Instant.now()
            val rawSensorData = RawSensorData(
                sensorType = sensorType,
                values = values,
                timestamp = timestamp,
                accuracy = accuracy,
                sync = false,
            )

            val rawSensorDataEntity = rawSensorData.toEntity()

            CoroutineScope(Dispatchers.IO).launch {
                if (!isCollecting) {
                    Log.d("HardwareModule", "Data collection is not active")
                    return@launch
                }
                Log.d("HardwareModule", "Data collection is Active\n${locationData?.latitude}")
                rawSensorDataRepository.insertRawSensorData(rawSensorDataEntity)
                locationData?.let {
                    locationRepository.insertLocation(it.toEntity())
                }

            }
//            CoroutineScope(Dispatchers.IO).launch {
//
//            }
            Unit // Explicitly return Unit
        }

        accelerometerSensor.whenSensorValueChangesListener(sensorEventListener)
        gyroscopeSensor.whenSensorValueChangesListener(sensorEventListener)
        rotationVectorSensor.whenSensorValueChangesListener(sensorEventListener)
        magnetometerSensor.whenSensorValueChangesListener(sensorEventListener)
        significantMotionSensor.whenSensorValueChangesListener(sensorEventListener)
        gravitySensor.whenSensorValueChangesListener(sensorEventListener)
        linearAccelerationSensor.whenSensorValueChangesListener(sensorEventListener)
    }

    init {
        setupListeners()
    }
}
