package com.uoa.sensor.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.PreferenceUtils
import com.uoa.sensor.location.LocationDataBufferManager
import com.uoa.sensor.utils.GetSensorTypeNameUtil
import com.uoa.sensor.utils.ProcessSensorData
import com.uoa.sensor.utils.ButterworthLowPassFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Date
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorRecordingManager @Inject constructor(
    private val accelerometerSensor: AccelerometerSensor,
    private val gyroscopeSensor: GyroscopeSensor,
    private val rotationVectorSensor: RotationVectorSensor,
    private val magnetometerSensor: MagnetometerSensor,
    private val gravitySensor: GravitySensor,
    private val linearAccelerationSensor: LinearAccelerationSensor,
    private val sensorDataBufferManager: SensorDataBufferManager,
    private val locationBufferManager: LocationDataBufferManager,
    private val context: Context
) {

    private val isRecording = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private var currentTripId: UUID? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Sensor Event Throttling
    private val MIN_DELAY_BETWEEN_EVENTS_MS = 20L
    private var lastProcessedEventTime = 0L
    private val SENSOR_SAMPLING_PERIOD_US = 20_000
    private val BUTTERWORTH_SAMPLING_HZ = 50.0
    private val BUTTERWORTH_CUTOFF_HZ = 20.0
    private val butterworthFilters = mutableMapOf<Int, ButterworthLowPassFilter>()

    // Rotation Matrix & Intermediate Reading
    private val rotationMatrix = FloatArray(9) { 0f }
    private val lastAccelerometer = FloatArray(3)
    private val lastMagnetometer = FloatArray(3)
    private var hasAccelerometerReading = false
    private var hasMagnetometerReading = false

    // Callback for sensor events
    private val sensorEventListener: (Int, List<Float>, Int) -> Unit = { sensorType, values, accuracy ->
        processSensorEvent(sensorType, values, accuracy)
    }

    /**
     * Starts collecting sensor data for the specified trip.
     */
    fun startRecording(tripId: UUID) {
        // If already recording, stop first to reset
        if (isRecording.get()) {
            Log.w("SensorRecordingManager", "Already recording. Restarting for new trip.")
            stopRecording()
        }
        
        currentTripId = tripId
        isRecording.set(true)
        isPaused.set(false)
        resetState()
        startSensorListeners()
        Log.d("SensorRecordingManager", "Started sensor recording for trip: $tripId")
    }

    /**
     * Stops collecting sensor data.
     */
    fun stopRecording() {
        if (isRecording.compareAndSet(true, false)) {
            isPaused.set(false)
            stopSensorListeners()
            // Flush any remaining data in buffer
            scope.launch {
                sensorDataBufferManager.flushBufferToDatabase()
            }
            Log.d("SensorRecordingManager", "Stopped sensor recording for trip: $currentTripId")
            currentTripId = null
        }
    }

    fun cleanup() {
        stopRecording()
        scope.cancel()
    }

    fun pauseRecording() {
        if (!isRecording.get()) return
        if (!isPaused.compareAndSet(false, true)) return
        stopSensorListeners()
        scope.launch {
            sensorDataBufferManager.flushBufferToDatabase()
        }
        Log.d("SensorRecordingManager", "Paused sensor recording for trip: $currentTripId")
    }

    fun resumeRecording() {
        if (!isRecording.get()) return
        if (!isPaused.compareAndSet(true, false)) return
        startSensorListeners()
        Log.d("SensorRecordingManager", "Resumed sensor recording for trip: $currentTripId")
    }

    private fun startSensorListeners() {
        try {
            accelerometerSensor.startListeningToSensor(SENSOR_SAMPLING_PERIOD_US)
            gyroscopeSensor.startListeningToSensor(SENSOR_SAMPLING_PERIOD_US)
            rotationVectorSensor.startListeningToSensor(SENSOR_SAMPLING_PERIOD_US)
            magnetometerSensor.startListeningToSensor(SENSOR_SAMPLING_PERIOD_US)
            gravitySensor.startListeningToSensor(SENSOR_SAMPLING_PERIOD_US)
            linearAccelerationSensor.startListeningToSensor(SENSOR_SAMPLING_PERIOD_US)

            accelerometerSensor.whenSensorValueChangesListener(sensorEventListener)
            gyroscopeSensor.whenSensorValueChangesListener(sensorEventListener)
            rotationVectorSensor.whenSensorValueChangesListener(sensorEventListener)
            magnetometerSensor.whenSensorValueChangesListener(sensorEventListener)
            gravitySensor.whenSensorValueChangesListener(sensorEventListener)
            linearAccelerationSensor.whenSensorValueChangesListener(sensorEventListener)
        } catch (e: Exception) {
            Log.e("SensorRecordingManager", "Error starting sensor listeners", e)
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
            Log.e("SensorRecordingManager", "Error stopping sensor listeners", e)
        }
    }

    private fun processSensorEvent(sensorType: Int, values: List<Float>, accuracy: Int) {
        val now = System.currentTimeMillis()
        if (now - lastProcessedEventTime < MIN_DELAY_BETWEEN_EVENTS_MS) {
            return
        }
        lastProcessedEventTime = now

        val sensorTypeName = GetSensorTypeNameUtil.getSensorTypeName(sensorType)

        // Maintain state for heading computation
        when (sensorTypeName) {
            "Accelerometer" -> {
                if (values.size >= 3) {
                    for (i in 0 until 3) lastAccelerometer[i] = values[i]
                    hasAccelerometerReading = true
                }
            }
            "Magnetometer" -> {
                if (values.size >= 3) {
                    for (i in 0 until 3) lastMagnetometer[i] = values[i]
                    hasMagnetometerReading = true
                }
            }
            "Rotation Vector" -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, values.toFloatArray())
            }
        }

        // If we have both accelerometer + magnetometer, compute heading/rotation matrix
        if (hasAccelerometerReading && hasMagnetometerReading) {
            SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)
        }

        if (isRecording.get()) {
            currentTripId?.let { tripId ->
                // Use Instant for timestamp (requires API 26+)
                val timestamp = Instant.now().toEpochMilli()
                // Replace non-finite values (NaN, ±Infinity) with 0.
                val validValues = values.map { if (it.isFinite()) it else 0f }.toFloatArray()

                // Apply any specific processing/transformation
                val processedValues = ProcessSensorData.processSensorData(sensorType, validValues, rotationMatrix)
                val filteredValues = applyButterworthFilter(sensorType, processedValues)

                // Filter out empty data
                if (!filteredValues.all { it == 0f }) {
                    val rawSensorData = RawSensorData(
                        id = UUID.randomUUID(),
                        sensorType = sensorType,
                        sensorTypeName = sensorTypeName,
                        values = filteredValues.toList(),
                        timestamp = timestamp,
                        date = Date(timestamp),
                        accuracy = accuracy,
                        locationId = locationBufferManager.getCurrentLocationId(),
                        tripId = tripId,
                        driverProfileId = PreferenceUtils.getDriverProfileId(context),
                        sync = false
                    )
                    sensorDataBufferManager.addToSensorBuffer(rawSensorData)
                }
            }
        }
    }

    private fun resetState() {
        hasAccelerometerReading = false
        hasMagnetometerReading = false
        lastProcessedEventTime = 0L
        butterworthFilters.values.forEach { it.reset() }
    }

    private fun applyButterworthFilter(sensorType: Int, values: FloatArray): FloatArray {
        return when (sensorType) {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val filter = butterworthFilters.getOrPut(sensorType) {
                    ButterworthLowPassFilter(BUTTERWORTH_SAMPLING_HZ, BUTTERWORTH_CUTOFF_HZ)
                }
                filter.filter(values)
            }
            else -> values
        }
    }
}
