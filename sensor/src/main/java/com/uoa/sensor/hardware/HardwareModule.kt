package com.uoa.sensor.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.PreferenceUtils
import com.uoa.sensor.location.LocationDataBufferManager
import com.uoa.sensor.location.LocationManager
import com.uoa.sensor.repository.SensorDataColStateRepository
import com.uoa.sensor.utils.GetSensorTypeNameUtil
import com.uoa.sensor.utils.ProcessSensorData
import kotlinx.coroutines.*
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HardwareModule @Inject constructor(
    private val accelerometerSensor: AccelerometerSensor,
    private val gyroscopeSensor: GyroscopeSensor,
    private val rotationVectorSensor: RotationVectorSensor,
    private val magnetometerSensor: MagnetometerSensor,
    private val gravitySensor: GravitySensor,
    private val linearAccelerationSensor: LinearAccelerationSensor,
    private val locationBufferManager: LocationDataBufferManager,
    private val locationManager: LocationManager,
    private val sensorDataBufferManager: SensorDataBufferManager,
    private val motionDetection: MotionDetectionFFT,
    private val sensorDataColStateRepository: SensorDataColStateRepository,
    private val context: Context
) : MotionDetectionFFT.MotionListener {

    // --------------------------------------
    // Collection State & Concurrency
    // --------------------------------------
    private val isCollecting = AtomicBoolean(false)
    private var currentTripId: UUID? = null

    // Background scope for async operations
    private val hardwareModuleScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --------------------------------------
    // Sensor Event Throttling
    // --------------------------------------
    private val MIN_DELAY_BETWEEN_EVENTS_MS = 100L  // Only process sensor data at most ~twice per second
    private var lastProcessedEventTime = 0L

    // --------------------------------------
    // Rotation Matrix & Intermediate Reading
    // --------------------------------------
    private val rotationMatrix = FloatArray(9) { 0f }
    private val lastAccelerometer = FloatArray(3)
    private val lastMagnetometer = FloatArray(3)
    private var hasAccelerometerReading = false
    private var hasMagnetometerReading = false

    // Callback that receives raw sensor updates from each sensor wrapper.
    private lateinit var sensorEventListener: (Int, List<Float>, Int) -> Unit

    init {
        setupListeners()
    }

    // --------------------------------------
    // Movement Detection Hooks
    // --------------------------------------
    override fun onMotionDetected() {
        hardwareModuleScope.launch {
            sensorDataColStateRepository.updateMovementStatus(true)
        }
    }

    override fun onMotionStopped() {
        hardwareModuleScope.launch {
            sensorDataColStateRepository.updateMovementStatus(false)
        }
    }

    /**
     * Start always-on motion detection (hybrid).
     */
    fun startMovementDetection() {
        motionDetection.addMotionListener(this)
        motionDetection.startHybridMotionDetection()
    }

    /**
     * Stop the always-on motion detection.
     */
    fun stopMovementDetection() {
        motionDetection.stopHybridMotionDetection()
        motionDetection.removeMotionListener(this)
    }

    // --------------------------------------
    // Trip Data Collection
    // --------------------------------------
    fun startDataCollection(tripId: UUID) {
        // If a collection is already active, stop it first (so we don't double-collect).
        if (isCollecting.get()) {
            Log.d("HardwareModule", "startDataCollection for trip: ${tripId} called, but we're already collecting. Stopping previous trip first.")
            stopDataCollection()
        }

        // Now mark new trip as active.
        currentTripId = tripId
        isCollecting.set(true)

        // Update local repository that trip is started.
        hardwareModuleScope.launch {
            sensorDataColStateRepository.startTripStatus(true)
            sensorDataColStateRepository.updateCollectionStatus(true)
        }

        // Start location & sensors for data collection.
        locationManager.startLocationUpdates()
        startSensorListeners()

        Log.d("HardwareModule", "Data collection started for trip: $tripId")
    }

    /**
     * Stop the current trip's data collection.
     * This flushes buffers, updates the DB & repository, and then clears references.
     */
    fun stopDataCollection() {
        hardwareModuleScope.launch {
            if (isCollecting.get()) {
                isCollecting.set(false)

                // Flush everything to local DB.
                sensorDataBufferManager.flushBufferToDatabase()
                locationBufferManager.flushBufferToDatabase()

                // Mark trip as stopped.
                sensorDataColStateRepository.startTripStatus(false)
                sensorDataColStateRepository.updateCollectionStatus(false)

                Log.d("HardwareModule", "Trip data flushed for trip: $currentTripId")
            }
        }
        // Cleanup & reset state
        clear()
    }

    // --------------------------------------
    // Sensor Listeners
    // --------------------------------------
    private fun startSensorListeners() {
        try {
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
            if (accelerometerSensor.doesSensorExist()) accelerometerSensor.stopListeningToSensor()
            if (gyroscopeSensor.doesSensorExist()) gyroscopeSensor.stopListeningToSensor()
            if (rotationVectorSensor.doesSensorExist()) rotationVectorSensor.stopListeningToSensor()
            if (magnetometerSensor.doesSensorExist()) magnetometerSensor.stopListeningToSensor()
            if (gravitySensor.doesSensorExist()) gravitySensor.stopListeningToSensor()
            if (linearAccelerationSensor.doesSensorExist()) linearAccelerationSensor.stopListeningToSensor()
        } catch (e: Exception) {
            Log.e("HardwareModule", "Error stopping sensor listeners", e)
        }
    }

    /**
     * Set up the shared sensor event listener that each sensor will invoke.
     */
    private fun setupListeners() {
        sensorEventListener = fun(sensorType: Int, values: List<Float>, accuracy: Int) {
            // Throttle sensor events to reduce CPU & battery usage
            val now = System.currentTimeMillis()
            if (now - lastProcessedEventTime < MIN_DELAY_BETWEEN_EVENTS_MS) {
                return
            }
            lastProcessedEventTime = now

            val sensorTypeName = GetSensorTypeNameUtil.getSensorTypeName(sensorType)

            // Maintain state for heading computation (Accelerometer + Magnetometer)
            when (sensorTypeName) {
                "Accelerometer" -> {
                    if (values.size >= 3) {
                        for (i in 0 until 3) {
                            lastAccelerometer[i] = values[i]
                        }
                        hasAccelerometerReading = true
                    }
                }
                "Magnetometer" -> {
                    if (values.size >= 3) {
                        for (i in 0 until 3) {
                            lastMagnetometer[i] = values[i]
                        }
                        hasMagnetometerReading = true
                    }
                }
                "Rotation Vector" -> {
                    updateRotationMatrixFromVector(values.toFloatArray())
                }
            }

            // If we have both accelerometer + magnetometer, compute heading
            if (hasAccelerometerReading && hasMagnetometerReading) {
                val success = SensorManager.getRotationMatrix(
                    rotationMatrix,
                    null,
                    lastAccelerometer,
                    lastMagnetometer
                )
                if (success) {
                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val headingDeg = Math.toDegrees(orientationAngles[0].toDouble())
                    Log.d("HardwareModule", "Heading: $headingDeg deg")
                }
            }

            // If collecting data for a trip, store these values
            if (isCollecting.get()) {
                currentTripId?.let { tripId ->
                    val timestamp = Instant.now().toEpochMilli()
                    // Replace non-finite values (NaN, ±Infinity) with 0.
                    val validValues = values.map { if (it.isFinite()) it else 0f }.toFloatArray()

                    val processedValues = processSensorData(sensorType, validValues, rotationMatrix)
                    if (processedValues.all { it == 0f }) {
                        // Filter out sensor events where all axes are 0
                        Log.d("HardwareModule", "Filtered out sensor reading with all zeros.")
                    } else {
                        val rawSensorData = RawSensorData(
                            id = UUID.randomUUID(),
                            sensorType = sensorType,
                            sensorTypeName = sensorTypeName,
                            values = processedValues.toList(),
                            timestamp = timestamp,
                            date = Date(timestamp),
                            accuracy = accuracy,
                            locationId = locationBufferManager.getCurrentLocationId(),
                            tripId = tripId,
                            driverProfileId = PreferenceUtils.getDriverProfileId(context),
                            sync = false
                        )
                        Log.d("HardwareModule", "Received sensor data for trip: ${rawSensorData.tripId}")
                        sensorDataBufferManager.addToSensorBuffer(rawSensorData)
                    }
                }
            }
        }

        // Register the event listener in each sensor manager
        try {
            accelerometerSensor.whenSensorValueChangesListener(sensorEventListener)
            gyroscopeSensor.whenSensorValueChangesListener(sensorEventListener)
            rotationVectorSensor.whenSensorValueChangesListener(sensorEventListener)
            magnetometerSensor.whenSensorValueChangesListener(sensorEventListener)
            gravitySensor.whenSensorValueChangesListener(sensorEventListener)
        } catch (e: Exception) {
            Log.e("HardwareModule", "Error setting up sensor listeners", e)
        }
    }

    // --------------------------------------
    // Utility & Cleanup
    // --------------------------------------
    private fun updateRotationMatrixFromVector(rotationValues: FloatArray) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationValues)
    }

    /**
     * Reset all state, stop listeners, and set isCollecting to false.
     * This is invoked after stopDataCollection.
     */
    private fun clear() {
        stopSensorListeners()
        locationManager.stopLocationUpdates()

        currentTripId = null
        isCollecting.set(false)

        hardwareModuleScope.launch {
            // In case any UI or logic depends on "collection status," ensure false
            sensorDataColStateRepository.updateCollectionStatus(false)
        }
    }

    /**
     * Optionally handle any sensor transformations (e.g., calibration, smoothing).
     */
    private fun processSensorData(
        sensorType: Int,
        inputValues: FloatArray,
        rotationMatrix: FloatArray
    ): FloatArray {
        return ProcessSensorData.processSensorData(sensorType, inputValues, rotationMatrix)
    }
}