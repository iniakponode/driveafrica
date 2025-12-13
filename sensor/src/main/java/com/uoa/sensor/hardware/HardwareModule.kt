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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
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
) : MotionDetectionFFT.MotionListener, com.uoa.sensor.motion.DrivingStateManager.StateCallback {

    // --------------------------------------
    // Collection State & Concurrency
    // --------------------------------------
    private val isCollecting = AtomicBoolean(false)
    private var currentTripId: UUID? = null

    // Expose current trip ID as StateFlow for UI observation
    private val _currentTripIdFlow = MutableStateFlow<UUID?>(null)

    /**
     * Flow of current trip ID for UI observation
     */
    fun currentTripIdFlow(): StateFlow<UUID?> = _currentTripIdFlow

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

    // Smart Motion Detection
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val drivingStateManager = com.uoa.sensor.motion.DrivingStateManager()

    init {
        setupListeners()

        // Initialize the smart motion detection
        drivingStateManager.initialize(this)
    }

    /**
     * Get the DrivingStateManager instance for UI monitoring
     */
    fun getDrivingStateManager(): com.uoa.sensor.motion.DrivingStateManager {
        return drivingStateManager
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
     * Start smart motion detection using DrivingStateManager FSM.
     * This uses low-power accelerometer monitoring in IDLE state,
     * and only enables GPS when vehicle motion is detected.
     */
    fun startMovementDetection() {
        Log.d("HardwareModule", "Starting smart motion detection with FSM")

        // Register accelerometer for motion detection
        val accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            sensorManager.registerListener(
                drivingStateManager,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            drivingStateManager.startMonitoring()

            hardwareModuleScope.launch {
                sensorDataColStateRepository.updateMovementType("Monitoring for vehicle motion")
            }

            Log.d("HardwareModule", "Smart motion detection started successfully")
        } else {
            Log.e("HardwareModule", "Accelerometer not available - motion detection failed")
        }
    }

    /**
     * Stop smart motion detection.
     */
    fun stopMovementDetection() {
        Log.d("HardwareModule", "Stopping smart motion detection")
        sensorManager.unregisterListener(drivingStateManager)
        drivingStateManager.stopMonitoring()

        hardwareModuleScope.launch {
            sensorDataColStateRepository.updateMovementType("Motion detection stopped")
        }
    }

    // =================================================================
    // DrivingStateManager.StateCallback Implementation
    // =================================================================

    override fun onDriveStarted() {
        Log.i("HardwareModule", "🚗 Drive detected and confirmed - Starting trip")

        // Create new trip automatically
        val newTripId = UUID.randomUUID()
        startDataCollection(newTripId)

        // Update UI state
        hardwareModuleScope.launch {
            sensorDataColStateRepository.updateMovementStatus(true)
            sensorDataColStateRepository.startTripStatus(true)
            sensorDataColStateRepository.updateMovementType("Recording trip")
        }
    }

    override fun onDriveStopped() {
        Log.i("HardwareModule", "🛑 Drive ended - Vehicle parked")

        // Stop data collection
        stopDataCollection()

        // Update UI state
        hardwareModuleScope.launch {
            sensorDataColStateRepository.updateMovementStatus(false)
            sensorDataColStateRepository.startTripStatus(false)
            sensorDataColStateRepository.updateMovementType("Waiting for motion")
        }
    }

    override fun requestGpsEnable() {
        Log.i("HardwareModule", "📍 Enabling GPS for verification")

        locationManager.startLocationUpdates()

        // Set up location callback to forward updates to DrivingStateManager
        locationManager.setLocationCallback { location ->
            drivingStateManager.updateLocation(location)
        }

        hardwareModuleScope.launch {
            sensorDataColStateRepository.updateMovementType("Verifying vehicle motion...")
        }
    }

    override fun requestGpsDisable() {
        Log.i("HardwareModule", "📍 Disabling GPS (battery save)")

        locationManager.stopLocationUpdates()

        hardwareModuleScope.launch {
            sensorDataColStateRepository.updateMovementType("Waiting for motion")
        }
    }

    override fun onStateChanged(newState: com.uoa.sensor.motion.DrivingStateManager.DrivingState) {
        Log.i("HardwareModule", "State changed: $newState")

        // Update UI with current state
        hardwareModuleScope.launch {
            val statusMessage = when (newState) {
                com.uoa.sensor.motion.DrivingStateManager.DrivingState.IDLE ->
                    "Waiting for motion"
                com.uoa.sensor.motion.DrivingStateManager.DrivingState.VERIFYING ->
                    "Verifying vehicle motion..."
                com.uoa.sensor.motion.DrivingStateManager.DrivingState.RECORDING ->
                    "Recording trip"
                com.uoa.sensor.motion.DrivingStateManager.DrivingState.POTENTIAL_STOP ->
                    "Vehicle stopped (traffic?)"
            }
            sensorDataColStateRepository.updateMovementType(statusMessage)
        }
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
        _currentTripIdFlow.value = tripId
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
        val tripId = currentTripId
        hardwareModuleScope.launch {
            if (isCollecting.get()) {
                isCollecting.set(false)

                // Flush everything to local DB.
                sensorDataBufferManager.flushBufferToDatabase()
                locationBufferManager.flushBufferToDatabase()

                // Mark trip as stopped.
                sensorDataColStateRepository.startTripStatus(false)
                sensorDataColStateRepository.updateCollectionStatus(false)

                Log.d("HardwareModule", "Trip data flushed for trip: $tripId")
            }
        }

        // Clear trip ID from flow
        _currentTripIdFlow.value = null

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
        try {
            stopSensorListeners()
            locationManager.stopLocationUpdates()

            // Stop buffer flush handler to prevent memory leaks
            sensorDataBufferManager.stopBufferFlushHandler()

            currentTripId = null
            isCollecting.set(false)

            // Reset sensor state
            hasAccelerometerReading = false
            hasMagnetometerReading = false
            lastProcessedEventTime = 0L

            hardwareModuleScope.launch {
                // In case any UI or logic depends on "collection status," ensure false
                sensorDataColStateRepository.updateCollectionStatus(false)
            }

            Log.d("HardwareModule", "Clear completed successfully")
        } catch (e: Exception) {
            Log.e("HardwareModule", "Error during clear", e)
        }
    }

    /**
     * Complete cleanup when module is destroyed.
     * MUST be called from Service onDestroy() to prevent memory leaks.
     */
    fun cleanup() {
        try {
            Log.d("HardwareModule", "Starting complete cleanup")

            // Stop any ongoing collection
            if (isCollecting.get()) {
                // Synchronous stop to ensure cleanup completes
                runBlocking {
                    stopDataCollection()
                }
            }

            // Stop motion detection
            try {
                stopMovementDetection()
                motionDetection.removeMotionListener(this)
            } catch (e: Exception) {
                Log.e("HardwareModule", "Error stopping motion detection", e)
            }

            // Cleanup buffers
            try {
                sensorDataBufferManager.cleanup()
            } catch (e: Exception) {
                Log.e("HardwareModule", "Error cleaning up buffer manager", e)
            }

            // Stop location
            try {
                locationManager.stopLocationUpdates()
            } catch (e: Exception) {
                Log.e("HardwareModule", "Error stopping location", e)
            }

            // Cancel coroutine scope
            try {
                hardwareModuleScope.cancel()
            } catch (e: Exception) {
                Log.e("HardwareModule", "Error cancelling scope", e)
            }

            Log.d("HardwareModule", "Complete cleanup finished")
        } catch (e: Exception) {
            Log.e("HardwareModule", "Error during cleanup", e)
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