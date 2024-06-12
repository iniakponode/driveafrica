package com.uoa.sensor.hardware

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.uoa.sensor.data.model.RawSensorData

import com.uoa.sensor.location.LocationManager
import com.uoa.sensor.utils.GetSensorTypeNameUtil
//import com.uoa.sensor.utils.ProcessSensorData
import com.uoa.sensor.utils.ProcessSensorData.logSensorValues
import com.uoa.sensor.utils.ProcessSensorData.processSensorData
//import com.uoa.sensor.utils.PermissionUtils
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class HardwareModule @Inject constructor(
    @AccelerometerSensorM private val accelerometerSensor: AccelerometerSensor,
    @GyroscopeSensorM private val gyroscopeSensor: GyroscopeSensor,
    @RotationVectorSensorM private val rotationVectorSensor: RotationVectorSensor,
    @MagnetometerSensorM private val magnetometerSensor: MagnetometerSensor,
    @SignificantMotionSensorM private val significantMotionSensor: SignificantMotion,
    @GravitySensorM private val gravitySensor: GravitySensor,
    @LinearAccelerationM private val linearAccelerationSensor: LinearAcceleration,
    private val locationManager: LocationManager,
    private val manageSensorDataSizeAndSave: ManageSensorDataSizeAndSave
) {

    private var isCollecting: Boolean = false
    private var currentTripId: UUID? = null
    private var currentLocationId: Long? = null
    fun startDataCollection(isLocationPermissionGranted: Boolean, tripId: UUID) {
        if (!isCollecting) {
            try {
                currentTripId = tripId
                if (isLocationPermissionGranted) {
                    Log.d("LocationPermission", "Starting location updates; Permission granted: $isLocationPermissionGranted")
                    locationManager.startLocationUpdates()
                }
                startSensorListeners()
                isCollecting = true
            } catch (e: Exception) {
                Log.e("HardwareModule", "Unexpected error while starting data collection", e)
            }
        }
    }

    fun stopDataCollection() {
        if (isCollecting) {
            try {
                locationManager.stopLocationUpdates()
                manageSensorDataSizeAndSave.processAndStoreSensorData()
                stopSensorListeners()
                isCollecting = false
                currentTripId = null
                currentLocationId = null
            } catch (e: Exception) {
                Log.e("HardwareModule", "Unexpected error while stopping data collection", e)
            }
        }
    }

    private fun startSensorListeners() {
        try {
            accelerometerSensor.startListeningToSensor(200)
            gyroscopeSensor.startListeningToSensor(200)
            rotationVectorSensor.startListeningToSensor(200)
            magnetometerSensor.startListeningToSensor(200)
            significantMotionSensor.startListeningToSensor(200)
            gravitySensor.startListeningToSensor(200)
            linearAccelerationSensor.startListeningToSensor(200)
        } catch (e: Exception) {
            Log.e("HardwareModule", "Error starting sensor listeners", e)
        }
    }

    private fun stopSensorListeners() {
        try {
            accelerometerSensor.stopListeningToSensor()
            gyroscopeSensor.stopListeningToSensor()
            rotationVectorSensor.stopListeningToSensor()
            magnetometerSensor.stopListeningToSensor()
            significantMotionSensor.stopListeningToSensor()
            gravitySensor.stopListeningToSensor()
            linearAccelerationSensor.stopListeningToSensor()
        } catch (e: Exception) {
            Log.e("HardwareModule", "Error stopping sensor listeners", e)
        }
    }

    private fun setupListeners() {
        val sensorEventListener: (Int, List<Float>, Int) -> Unit = { sensorType, values, accuracy ->
            val timestamp = Instant.now()
            val sensorTypeName = GetSensorTypeNameUtil.getSensorTypeName(sensorType)

            val validValues = values.map { if (it.isFinite()) it else 0f }  // Ensure values are valid floats

            // Log sensor values for debugging
            logSensorValues(sensorTypeName, validValues, "Unprocessed")

            val processedValues = processSensorData(sensorType, validValues.toFloatArray())

            // Log processed sensor values for debugging
            logSensorValues(sensorTypeName, processedValues.toList(), "Processed")
            val rawSensorData = RawSensorData(
                id= UUID.randomUUID(),
                sensorType = sensorType,
                sensorTypeName = sensorTypeName,
                values = processedValues.toList(),
                timestamp = timestamp,
                accuracy = accuracy,
                locationId = locationManager.getCurrentLocationId(),  // Get the current location ID
                tripId = currentTripId,
                sync = false,
            )

            if (isCollecting) {
                manageSensorDataSizeAndSave.addToSensorDataBuffer(rawSensorData)
            } else {
                Log.d("HardwareModule", "Data collection is not active")
            }
        }

        try {
            accelerometerSensor.whenSensorValueChangesListener(sensorEventListener)
            gyroscopeSensor.whenSensorValueChangesListener(sensorEventListener)
            rotationVectorSensor.whenSensorValueChangesListener(sensorEventListener)
            magnetometerSensor.whenSensorValueChangesListener(sensorEventListener)
            significantMotionSensor.whenSensorValueChangesListener(sensorEventListener)
            gravitySensor.whenSensorValueChangesListener(sensorEventListener)
            linearAccelerationSensor.whenSensorValueChangesListener(sensorEventListener)
        } catch (e: Exception) {
            Log.e("HardwareModule", "Error setting up sensor listeners", e)
        }
    }

    init {
        setupListeners()
    }
}