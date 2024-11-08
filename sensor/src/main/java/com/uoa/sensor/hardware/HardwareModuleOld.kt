//package com.uoa.sensor.hardware
//
//import android.hardware.Sensor
//import android.hardware.SensorManager
//import android.location.Location
//import android.location.LocationListener
//import android.os.Build
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import androidx.annotation.RequiresApi
//import com.uoa.core.model.RawSensorData
//import com.uoa.ml.domain.BatchInsertCauseUseCase
//import com.uoa.ml.domain.BatchUpDateUnsafeBehaviourCauseUseCase
//import com.uoa.ml.domain.RunClassificationUseCase
//import com.uoa.ml.domain.SaveInfluenceToCause
//import com.uoa.ml.domain.UpDateUnsafeBehaviourCauseUseCase
//import com.uoa.sensor.domain.usecases.trip.UpdateTripUseCase
//
//import com.uoa.sensor.location.LocationManager
//import com.uoa.sensor.location.LocationManager.VehicleMotionListener
//import com.uoa.sensor.utils.GetSensorTypeNameUtil
//import com.uoa.sensor.utils.ProcessSensorData.logSensorValues
//import com.uoa.sensor.utils.ProcessSensorData.processSensorData
////import com.uoa.dbda.util.ProcessSensorData
////import com.uoa.sensor.utils.PermissionUtils
//import java.time.Instant
//import java.util.Date
//import java.util.Timer
//import java.util.TimerTask
//import java.util.UUID
//import javax.inject.Inject
//import javax.inject.Singleton
//import kotlin.math.pow
//import kotlin.math.sqrt
//
//@Singleton
//class HardwareModuleOld @Inject constructor(
//    @AccelerometerSensorM private val accelerometerSensor: AccelerometerSensor,
//    @GyroscopeSensorM private val gyroscopeSensor: GyroscopeSensor,
//    @RotationVectorSensorM private val rotationVectorSensor: RotationVectorSensor,
//    @MagnetometerSensorM private val magnetometerSensor: MagnetometerSensor,
//    @SignificantMotionSensorM private val significantMotionSensor: SignificantMotion,
//    @GravitySensorM private val gravitySensor: GravitySensor,
////    @LinearAccelerationM private val linearAccelerationSensor: LinearAcceleration,
//    private val locationManager: LocationManager,
//    private val manageSensorDataSizeAndSave: ManageSensorDataSizeAndSave,
//    private val runClassificationUseCase: RunClassificationUseCase,
//    private val upDateUnsafeBehaviourCauseUseCase: UpDateUnsafeBehaviourCauseUseCase,
//    private val saveInfluenceToCause: SaveInfluenceToCause,
//    private val batchInsertCauseUseCase: BatchInsertCauseUseCase,
//    private val batchUpDateUnsafeBehaviourCauseUseCase: BatchUpDateUnsafeBehaviourCauseUseCase,
//    private val updateTripUseCase: UpdateTripUseCase
//) {
//
//    private var isCollecting: Boolean = false
//    private var currentTripId: UUID? = null
//    private var currentLocationId: Long? = null
//    private var rotationMatrix = FloatArray(9) { 0f }
//
//    //    Significant motion detection
//    private var isSignificantMotionDetected = false
//    private var isVehicleMoving = false
//    private val ACCELERATION_THRESHOLD = 0.9f // Threshold for vehicle movement
//    private val SIGNIFICANT_MOTION_WAIT_DURATION = 5000L // Time to wait for significant motion detection (in milliseconds)
//
//    // Update rotation matrix whenever rotation vector changes
//    private fun updateRotationMatrix(rotationValues: FloatArray) {
//        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationValues)
//    }
//
//    fun isCollectingData(): Boolean {
//        return isCollecting
//    }
//
//    // Start hybrid motion detection
//    fun startHybridMotionDetection() {
//        try {
//            // Function to handle motion detection from various sources
//            fun onMotionDetected(source: String) {
//                Log.d("HardwareModule", "$source: Motion detected, starting sensor listeners")
//                isVehicleMoving = true
//                isCollecting = true
//                startSensorListeners()  // Now uncommented
//
//                // Stop the corresponding sensor/listener
//                when (source) {
//                    "GPS" -> locationManager.stopLocationUpdates()
//                    "SignificantMotionSensor" -> significantMotionSensor.stopListeningToSensor()
//                }
//            }
//
//            // Set vehicle motion listener for GPS verification
//            locationManager.setVehicleMotionListener(object : VehicleMotionListener {
//                override fun onVehicleMotionDetected() {
//                    onMotionDetected("GPS")
//                }
//
//                override fun onVehicleMotionStopped() {
//                    Log.d("HardwareModule", "GPS: Vehicle motion stopped")
//                }
//            })
//
//            // Start listening for significant motion
//            significantMotionSensor.setOnSensorValuesChangedListener {
//                isSignificantMotionDetected = true
//                onMotionDetected("SignificantMotionSensor")
//            }
//
//            significantMotionSensor.startListeningToSensor(200)
//            Log.d("HardwareModule", "Significant motion sensor listening started")
//
//            // Fallback to accelerometer detection if significant motion is not detected after a certain duration
//            Handler(Looper.getMainLooper()).postDelayed({
//                if (!isSignificantMotionDetected) {
//                    Log.d(
//                        "HardwareModule",
//                        "No significant motion detected, starting fallback with accelerometer"
//                    )
//                    isVehicleMoving = true
//                    isCollecting = true
//                    startFallbackAccelerometerDetection()
//                }
//            }, SIGNIFICANT_MOTION_WAIT_DURATION)
//
//            // Start GPS verification to check for vehicle movement
//            locationManager.startLocationUpdates()
//
//        } catch (e: Exception) {
//            Log.e("HardwareModule", "Error initializing hybrid motion detection", e)
//        }
//    }
//
//
//    // Fallback accelerometer detection
//    private fun startFallbackAccelerometerDetection() {
//        accelerometerSensor.setOnSensorValuesChangedListener { values ->
//            // Use raw accelerometer data before movement is detected
//            Log.d("RawData", "Raw accelerometer values: ${values.joinToString()}")
//
//            // Calculate the magnitude of acceleration using raw values
//            val magnitude = sqrt(
//                values[0].pow(2) +
//                        values[1].pow(2) +
//                        values[2].pow(2)
//            )
//
//            Log.d("RawData", "Calculated magnitude: $magnitude")
//
//            if (magnitude > ACCELERATION_THRESHOLD) {
//                Log.d(
//                    "Fallback",
//                    "Fallback: Movement detected using accelerometer, starting sensor listeners"
//                )
//                isSignificantMotionDetected = true
//                isVehicleMoving = true
//                isCollecting = true
//            }
//        }
//
//        // Start listening to accelerometer
//        startSensorListeners()
//    }
//
//    private fun setupListeners() {
//        val sensorEventListener: (Int, List<Float>, Int) -> Unit = { sensorType, values, accuracy ->
//            val sensorTypeName = GetSensorTypeNameUtil.getSensorTypeName(sensorType)
//            Log.d("SensorData", "$sensorTypeName values: ${values.joinToString()}")
//
//            if (sensorTypeName == "Rotation Vector") {
//                // Update the rotation matrix when we get new values from the rotation vector
//                updateRotationMatrix(values.toFloatArray())
//                Log.d("RotationMatrix", "Updated rotation matrix: ${rotationMatrix.joinToString()}")
//            } else {
//                val timestamp = Instant.now().toEpochMilli()
//                val sensorTypeName = GetSensorTypeNameUtil.getSensorTypeName(sensorType)
//
//                val validValues = values.map { if (it.isFinite()) it else 0f }
//
//                // Process sensor data and transform it based on rotation matrix
//                val processedValues = processSensorData(sensorType, validValues.toFloatArray(), rotationMatrix)
//
//                val rawSensorData = RawSensorData(
//                    id = UUID.randomUUID(),
//                    sensorType = sensorType,
//                    sensorTypeName = sensorTypeName,
//                    values = processedValues.toList(),
//                    timestamp = timestamp,
//                    date = Date(timestamp),
//                    accuracy = accuracy,
//                    locationId = locationManager.getCurrentLocationId(),
//                    tripId = currentTripId,
//                    sync = false,
//                )
//
//                if (isCollecting) {
//                    Log.d("SensorModule", "Data collection is active")
//                    manageSensorDataSizeAndSave.addToSensorDataBuffer(rawSensorData)
//                } else {
//                    Log.d("SensorModule", "Data collection is not active")
//                }
//            }
//        }
//
//        try {
//            accelerometerSensor.whenSensorValueChangesListener(sensorEventListener)
//            gyroscopeSensor.whenSensorValueChangesListener(sensorEventListener)
//            rotationVectorSensor.whenSensorValueChangesListener(sensorEventListener)  // Rotation vector listener
//            magnetometerSensor.whenSensorValueChangesListener(sensorEventListener)
//            gravitySensor.whenSensorValueChangesListener(sensorEventListener)
//        } catch (e: Exception) {
//            Log.e("SensorModule", "Error setting up sensor listeners", e)
//        }
//    }
//
//
//
//    fun startDataCollection(isLocationPermissionGranted: Boolean, tripId: UUID) {
//        if (!isCollecting) {
//            try {
//                currentTripId = tripId
//                if (isLocationPermissionGranted) {
//                    Log.d(
//                        "LocationPermission",
//                        "Starting location updates; Permission granted: $isLocationPermissionGranted"
//                    )
//                    locationManager.startLocationUpdates()
//                }
//                // Initiate hybrid motion detection without relying on its return value
//                startHybridMotionDetection()
//                Log.d("SensorModule", "Data collection initiated")
//
//            } catch (e: Exception) {
//                Log.e("SensorModule", "Unexpected error while starting data collection", e)
//            }
//        } else {
//            Log.d("SensorModule", "Data collection is already in progress")
//        }
//    }
//
//
//    fun stopDataCollection() {
//        if (isCollecting) {
//            try {
//                locationManager.stopLocationUpdates()
//                manageSensorDataSizeAndSave.processAndStoreSensorData()
//                stopSensorListeners()
//                isCollecting = false
//
//                updateTripUseCase.invoke(currentTripId!!)
//                // Run classification
//                val influence = runClassificationUseCase.invoke(currentTripId!!)
//                Log.d("AlcoholIn", "Influence: $influence")
////                upDateUnsafeBehaviourCauseUseCase.invoke(currentTripId!!, influence)
//                batchUpDateUnsafeBehaviourCauseUseCase.invoke(currentTripId!!, influence)
//                Log.d("AlcoholIn", "Influence updated to Unsafe Behaviour table")
//
//
//
//                currentTripId = null
//                currentLocationId = null
//            } catch (e: Exception) {
//                Log.e("HardwareModule", "Unexpected error while stopping data collection", e)
//            }
//        }
//    }
//
//
//
//    // Start Sensor Listeners
//    private fun startSensorListeners() {
//        try {
//
//            accelerometerSensor.startListeningToSensor(200)
//            gyroscopeSensor.startListeningToSensor(200)
//            rotationVectorSensor.startListeningToSensor(200)
//            magnetometerSensor.startListeningToSensor(200)
////            significantMotionSensor.startListeningToSensor(200)
//            gravitySensor.startListeningToSensor(200)
////            linearAccelerationSensor.startListeningToSensor(200)
//        } catch (e: Exception) {
//            Log.e("HardwareModule", "Error starting sensor listeners", e)
//        }
//    }
//
//
//    private fun stopSensorListeners() {
//        try {
//            accelerometerSensor.stopListeningToSensor()
//            gyroscopeSensor.stopListeningToSensor()
//            rotationVectorSensor.stopListeningToSensor()
//            magnetometerSensor.stopListeningToSensor()
//            significantMotionSensor.stopListeningToSensor()
//            gravitySensor.stopListeningToSensor()
////            linearAccelerationSensor.stopListeningToSensor()
//
//        } catch (e: Exception) {
//            Log.e("HardwareModule", "Error stopping sensor listeners", e)
//        }
//    }
//
//    init {
//        setupListeners()
//    }
//}