package com.uoa.sensor.hardware

import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.uoa.core.model.RawSensorData
import com.uoa.ml.domain.BatchInsertCauseUseCase
import com.uoa.ml.domain.BatchUpDateUnsafeBehaviourCauseUseCase
import com.uoa.ml.domain.RunClassificationUseCase
import com.uoa.ml.domain.SaveInfluenceToCause
import com.uoa.ml.domain.UpDateUnsafeBehaviourCauseUseCase
import com.uoa.sensor.domain.usecases.trip.UpdateTripUseCase
import com.uoa.sensor.hardware.base.SignificantMotionSensor

import com.uoa.sensor.location.LocationManager
import com.uoa.sensor.location.LocationManager.VehicleMotionListener
import com.uoa.sensor.utils.GetSensorTypeNameUtil
import com.uoa.sensor.utils.ProcessSensorData.logSensorValues
import com.uoa.sensor.utils.ProcessSensorData.processSensorData
//import com.uoa.dbda.util.ProcessSensorData
//import com.uoa.sensor.utils.PermissionUtils
import java.time.Instant
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt


@Singleton
class HardwareModule @Inject constructor(
    @AccelerometerSensorM private val accelerometerSensor: AccelerometerSensor,
    @GyroscopeSensorM private val gyroscopeSensor: GyroscopeSensor,
    @RotationVectorSensorM private val rotationVectorSensor: RotationVectorSensor,
    @MagnetometerSensorM private val magnetometerSensor: MagnetometerSensor,
    @SignificantMotionSensorM private val significantMotionSensor: SignificantMotionSensor,
    @GravitySensorM private val gravitySensor: GravitySensor,
    @LinearAccelerationM private val linearAccelerationSensor: LinearAccelerationSensor,
    private val locationManager: LocationManager,
    private val manageSensorDataSizeAndSave: ManageSensorDataSizeAndSave,
    private val runClassificationUseCase: RunClassificationUseCase,
    private val upDateUnsafeBehaviourCauseUseCase: UpDateUnsafeBehaviourCauseUseCase,
    private val saveInfluenceToCause: SaveInfluenceToCause,
    private val batchInsertCauseUseCase: BatchInsertCauseUseCase,
    private val batchUpDateUnsafeBehaviourCauseUseCase: BatchUpDateUnsafeBehaviourCauseUseCase,
    private val updateTripUseCase: UpdateTripUseCase
) {

    private var isCollecting: Boolean = false
    private var currentTripId: UUID? = null
    private var currentLocationId: Long? = null
    private var rotationMatrix = FloatArray(9) { 0f }

    private var fallbackHandler: Handler? = null
    private var fallbackRunnable: Runnable? = null

    // Significant motion detection flags
    private var isSignificantMotionDetected = false
    private var isVehicleMoving = false
    private val ACCELERATION_THRESHOLD = 0.5f
    private val LINEAR_ACCELERATION_THRESHOLD = 0.5f // Adjusted threshold for vehicle movement
    private val SIGNIFICANT_MOTION_WAIT_DURATION = 5000L // Time to wait for significant motion detection (in milliseconds)



    // Update rotation matrix whenever rotation vector changes
    private fun updateRotationMatrix(rotationValues: FloatArray) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationValues)
    }

    fun isVehicleMoving(): Boolean {
        return isVehicleMoving
    }

    // Start hybrid motion detection
    fun startHybridMotionDetection() {
        try {
            // Set vehicle motion listener for GPS verification
            locationManager.setVehicleMotionListener(object : VehicleMotionListener {
                override fun onVehicleMotionDetected() {
                    onMotionDetected("GPS")
                }

                override fun onVehicleMotionStopped() {
                    Log.d("HardwareModule", "GPS: Vehicle motion stopped")
                }
            })

            var significantMotionSensorStarted = false

            // Start listening for significant motion if sensor exists
            if (significantMotionSensor.doesSensorExist()) {
                significantMotionSensor.setOnTriggerListener {
                    isSignificantMotionDetected = true
                    onMotionDetected("SignificantMotionSensor")
                }

                significantMotionSensorStarted = significantMotionSensor.startListeningToSensor()
                if (significantMotionSensorStarted) {
                    Log.d("HardwareModule", "Significant motion sensor listening started")
                } else {
                    Log.e("HardwareModule", "Failed to start significant motion sensor listener")
                }
            } else {
                Log.e("HardwareModule", "Significant Motion Sensor not available on this device.")
            }

            // Fallback to linear acceleration detection if significant motion sensor is not available or not triggered within the wait duration
            if (!significantMotionSensorStarted) {
                Log.d(
                    "HardwareModule",
                    "Significant Motion Sensor not available or failed to start, starting fallback with linear acceleration sensor"
                )
                startFallbackLinearAccelerationDetection()
            } else {
                // Fallback after waiting duration if no significant motion detected
                fallbackHandler = Handler(Looper.getMainLooper())
                fallbackRunnable = Runnable {
                    if (!isSignificantMotionDetected) {
                        Log.d(
                            "HardwareModule",
                            "No significant motion detected within wait duration, starting fallback with linear acceleration sensor"
                        )
                        startFallbackLinearAccelerationDetection()
                    }
                }

                fallbackHandler?.postDelayed(fallbackRunnable!!, SIGNIFICANT_MOTION_WAIT_DURATION)
            }

            // Start GPS verification to check for vehicle movement
            locationManager.startLocationUpdates()

        } catch (e: Exception) {
            Log.e("HardwareModule", "Error initializing hybrid motion detection", e)
        }
    }

    // Fallback linear acceleration detection
    private fun startFallbackLinearAccelerationDetection() {
        try {
            Log.d("Fallback", "Starting fallback linear acceleration detection")

            if (!linearAccelerationSensor.doesSensorExist()) {
                Log.e("Fallback", "Linear Acceleration Sensor not available on this device.")
                // As a last resort, fallback to accelerometer
                startFallbackAccelerometerDetection()
                return
            }

            // Set the sensor event listener
            linearAccelerationSensor.whenSensorValueChangesListener(fallbackSensorEventListener)

            // Start listening to linear acceleration sensor
            val sensorStarted = linearAccelerationSensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
            if (sensorStarted) {
                Log.d("Fallback", "Linear acceleration sensor started for fallback detection")
            } else {
                Log.e("Fallback", "Failed to start linear acceleration sensor for fallback detection")
                // As a last resort, fallback to accelerometer
                startFallbackAccelerometerDetection()
            }
        } catch (e: Exception) {
            Log.e("Fallback", "Error in fallback linear acceleration detection", e)
        }
    }

    // Fallback accelerometer detection (as a last resort)
    private fun startFallbackAccelerometerDetection() {
        try {
            Log.d("Fallback", "Starting fallback accelerometer detection")

            if (!accelerometerSensor.doesSensorExist()) {
                Log.e("Fallback", "Accelerometer Sensor not available on this device.")
                return
            }

            // Set the sensor event listener
            accelerometerSensor.whenSensorValueChangesListener(fallbackSensorEventListener)

            // Start listening to accelerometer
            val sensorStarted = accelerometerSensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
            if (sensorStarted) {
                Log.d("Fallback", "Accelerometer sensor started for fallback detection")
            } else {
                Log.e("Fallback", "Failed to start accelerometer sensor for fallback detection")
            }
        } catch (e: Exception) {
            Log.e("Fallback", "Error in fallback accelerometer detection", e)
        }
    }

    private val fallbackSensorEventListener = fun(sensorType: Int, values: List<Float>, accuracy: Int) {
        if (isVehicleMoving) {
            // Movement already detected, do not process further
            return
        }
        try {
            val sensorTypeName = GetSensorTypeNameUtil.getSensorTypeName(sensorType)
            // Process the sensor data before calculating magnitude
            Log.d("Fallback", "Raw $sensorTypeName values: ${values.joinToString()}")
            val processedValues = processSensorData(
                sensorType,
                values.toFloatArray(),
                rotationMatrix
            )
            Log.d("Fallback", "Processed $sensorTypeName values: ${processedValues.joinToString()}")
            val magnitude = sqrt(
                processedValues[0].pow(2) +
                processedValues[1].pow(2) +
                processedValues[2].pow(2)
            )

            Log.d("Fallback", "Calculated magnitude: $magnitude")

            if (sensorType == Sensor.TYPE_LINEAR_ACCELERATION && magnitude < LINEAR_ACCELERATION_THRESHOLD) {
                Log.d(
                    "Fallback",
                    "Fallback: Movement detected using linear acceleration, starting sensor listeners"
                )
                isSignificantMotionDetected = true
                onMotionDetected("LinearAccelerationSensor")
                // Do not stop the sensor here; we want to keep collecting data
            } else if (sensorType == Sensor.TYPE_ACCELEROMETER && (magnitude - 9.81f) < ACCELERATION_THRESHOLD) {
                Log.d(
                    "Fallback",
                    "Fallback: Movement detected using accelerometer, starting sensor listeners"
                )
                isSignificantMotionDetected = true
                onMotionDetected("AccelerometerSensor")
                // Do not stop the sensor here; we want to keep collecting data
            }
            else{
                isVehicleMoving=false
            }
        } catch (e: Exception) {
            Log.e("Fallback", "Error inside fallback sensor listener", e)
        }
    }


    private fun onMotionDetected(source: String) {
        Log.d("HardwareModule", "$source: Motion detected, starting sensor listeners")
        isVehicleMoving = true
//        isCollecting = true // Start collecting data from this point

        // Start other sensor listeners
        startSensorListeners()

        // Replace the fallback sensor event listener with the main one
        when (source) {
            "LinearAccelerationSensor" -> linearAccelerationSensor.whenSensorValueChangesListener(sensorEventListener)
            "AccelerometerSensor" -> accelerometerSensor.whenSensorValueChangesListener(sensorEventListener)
        }
    }

    private lateinit var sensorEventListener: (Int, List<Float>, Int) -> Unit

    private fun setupListeners() {
        sensorEventListener = { sensorType, values, accuracy ->
            val sensorTypeName = GetSensorTypeNameUtil.getSensorTypeName(sensorType)
            if (sensorTypeName == "Rotation Vector") {
                // Update the rotation matrix when we get new values from the rotation vector
                updateRotationMatrix(values.toFloatArray())
            } else {
                val timestamp = Instant.now().toEpochMilli()

                val validValues = values.map { if (it.isFinite()) it else 0f }

                // Process sensor data and transform it based on rotation matrix
                val processedValues = processSensorData(sensorType, validValues.toFloatArray(), rotationMatrix)

                val rawSensorData = RawSensorData(
                    id = UUID.randomUUID(),
                    sensorType = sensorType,
                    sensorTypeName = sensorTypeName,
                    values = processedValues.toList(),
                    timestamp = timestamp,
                    date = Date(timestamp),
                    accuracy = accuracy,
                    locationId = locationManager.getCurrentLocationId(),
                    tripId = currentTripId,
                    sync = false,
                )

                if (isCollecting) {
                    Log.d("SensorModule", "Data collection is active")
                    manageSensorDataSizeAndSave.addToSensorDataBufferAndSave(rawSensorData)
                } else {
                    Log.d("SensorModule", "Data collection is not active")
                }
            }
        }

        try {
            accelerometerSensor.whenSensorValueChangesListener(sensorEventListener)
            gyroscopeSensor.whenSensorValueChangesListener(sensorEventListener)
            rotationVectorSensor.whenSensorValueChangesListener(sensorEventListener)
            magnetometerSensor.whenSensorValueChangesListener(sensorEventListener)
            gravitySensor.whenSensorValueChangesListener(sensorEventListener)
            linearAccelerationSensor.whenSensorValueChangesListener(sensorEventListener)
        } catch (e: Exception) {
            Log.e("SensorModule", "Error setting up sensor listeners", e)
        }
    }

    fun startDataCollection(isLocationPermissionGranted: Boolean, tripId: UUID) {

        if (!isCollecting) {
            try {
                isCollecting=true
                currentTripId = tripId
                if (isLocationPermissionGranted) {
                    Log.d(
                        "LocationPermission",
                        "Starting location updates; Permission granted: $isLocationPermissionGranted"
                    )
                    locationManager.startLocationUpdates()
                }
                // isCollecting remains false until movement is detected
                // Initiate hybrid motion detection
                startHybridMotionDetection()
                Log.d("SensorModule", "Data collection initiated")
            } catch (e: Exception) {
                Log.e("SensorModule", "Unexpected error while starting data collection", e)
            }
        } else {
            Log.d("SensorModule", "Data collection is already in progress")
        }
    }

    suspend fun stopDataCollection() {
        if (isCollecting || isVehicleMoving) {
            try {
                locationManager.stopLocationUpdates()
                manageSensorDataSizeAndSave.processSensorData()
                stopSensorListeners()

                isCollecting = false

                // Cancel any pending fallback tasks
                fallbackHandler?.removeCallbacks(fallbackRunnable!!)
                fallbackHandler = null
                fallbackRunnable = null

                // Run classification
                val alcInfluence = runClassificationUseCase.invoke(currentTripId!!)
                if (alcInfluence)
                    updateTripUseCase.invoke(currentTripId!!, "alcohol")
                else
                    updateTripUseCase.invoke(currentTripId!!, "No influence")

                Log.d("AlcoholIn", "Influence: $alcInfluence")
                // batchUpDateUnsafeBehaviourCauseUseCase.invoke(currentTripId!!, alcInfluence)
                Log.d("AlcoholIn", "Influence updated to Unsafe Behaviour table")

                currentTripId = null
                currentLocationId = null
            } catch (e: Exception) {
                Log.e("HardwareModule", "Error during classification", e)
            }
        }
    }

    // Start Sensor Listeners
    private fun startSensorListeners() {
        try {
            // Start all sensors
            accelerometerSensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
            gyroscopeSensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
            rotationVectorSensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
            magnetometerSensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
            gravitySensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
            linearAccelerationSensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
        } catch (e: Exception) {
            Log.e("HardwareModule", "Error starting sensor listeners", e)
        }
    }

    private fun stopSensorListeners() {
        try {
            if (accelerometerSensor.doesSensorExist())
                accelerometerSensor.stopListeningToSensor()
            else
                Log.d("Sensor", "Acceleration Sensor Does not Exists")
            if (gyroscopeSensor.doesSensorExist())
                gyroscopeSensor.stopListeningToSensor()
            else
                Log.d("Sensor", "gyroscope Sensor Does not Exists")

            if (rotationVectorSensor.doesSensorExist())
                rotationVectorSensor.stopListeningToSensor()
            else
                Log.d("Sensor", "rotationVector Sensor Does not Exists")
            if (magnetometerSensor.doesSensorExist())
                magnetometerSensor.stopListeningToSensor()
            else
                Log.d("Sensor", "Magnetometer Sensor Does not Exists")

            if (significantMotionSensor.doesSensorExist())
                significantMotionSensor.stopListeningToSensor()
            else
                Log.d("Sensor", "Significant Motion Sensor Does not Exists")
            if (gravitySensor.doesSensorExist())
                gravitySensor.stopListeningToSensor()
            else
                Log.d("Sensor", "Gravity Sensor Does not Exists")
            if (linearAccelerationSensor.doesSensorExist())
                linearAccelerationSensor.stopListeningToSensor()
            else
                Log.d("Sensor", "Linear Acceleration Sensor Does not Exists")
        } catch (e: Exception) {
            Log.e("HardwareModule", "Error stopping sensor listeners", e)
        }
    }

    init {
        setupListeners()
    }
}