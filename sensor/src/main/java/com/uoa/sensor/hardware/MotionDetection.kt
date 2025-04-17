package com.uoa.sensor.hardware

import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import com.uoa.sensor.hardware.base.SignificantMotionSensor
import com.uoa.sensor.repository.SensorDataColStateRepository
import com.uoa.sensor.utils.ProcessSensorData
import kotlinx.coroutines.*
import kotlin.math.pow
import kotlin.math.sqrt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MotionDetection @Inject constructor(
    private val significantMotionSensor: SignificantMotionSensor,
    private val linearAccelerationSensor: LinearAccelerationSensor,
    private val accelerometerSensor: AccelerometerSensor,
    private val sensorDataColStateRepository: SensorDataColStateRepository
) {

    /**
     * Listeners that receive callbacks when motion starts or stops.
     * Ideal for hooking into the rest of your app (e.g., start trip logic).
     */
    interface MotionListener {
        fun onMotionDetected()
        fun onMotionStopped()
    }

    // --------------------
    //  CONFIGURABLE CONSTANTS
    // --------------------

    // Threshold for deciding that linear acceleration = "movement".
    private val LINEAR_ACCELERATION_THRESHOLD = 0.3f

    // If we do have a significant motion sensor, wait a few seconds before fallback.
    private val SIGNIFICANT_MOTION_WAIT_DURATION = 5_000L

    // Debounce interval for motion transitions (to ignore small fluctuations).
    private val DEBOUNCE_INTERVAL = 10_000L

    // If no motion occurs for this long, we stop fallback detection.
    // Using 5 minutes to preserve battery. Adjust to your use case.
    private val INACTIVITY_TIMEOUT = 300_000L

    // After inactivity, how long to wait before resuming fallback detection.
    // Using 5 minutes here as well.
    private val RESUME_DELAY = 300_000L

    // Throttle sensor processing to at most once per second.
    private val MIN_DELAY_BETWEEN_PROCESSES = 1_000L

    // --------------------
    // INTERNAL STATE
    // --------------------
    private val motionDetectorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Jobs to control fallback timers, debouncing, inactivity checks, etc.
    private var fallbackJob: Job? = null
    private var debounceJob: Job? = null
    private var inactivityCheckJob: Job? = null

    // Track whether fallback detection is active, and whether the vehicle is moving.
    private var isFallbackActive = false
    private var isVehicleMoving = false

    // Track timestamps for inactivity checks and debouncing.
    private var lastMotionTimestamp: Long = System.currentTimeMillis()
    private var lastMotionChangeTimestamp: Long = 0L

    // Keep track of last time we processed a sensor reading (for throttle).
    private var lastProcessTime = 0L

    private val motionListeners = mutableListOf<MotionListener>()

    // --------------------
    //  PUBLIC API
    // --------------------

    /**
     * Adds a listener that will be notified when motion starts or stops.
     */
    fun addMotionListener(listener: MotionListener) {
        if (!motionListeners.contains(listener)) {
            motionListeners.add(listener)
        }
    }

    /**
     * Removes a previously added motion listener.
     */
    fun removeMotionListener(listener: MotionListener) {
        motionListeners.remove(listener)
    }

    /**
     * Starts motion detection in "hybrid" mode:
     *  - Uses significant-motion sensor if available (very low power).
     *  - If not triggered within SIGNIFICANT_MOTION_WAIT_DURATION, fall back to
     *    normal sensors at SENSOR_DELAY_NORMAL (higher power).
     */
    fun startHybridMotionDetection() {
        motionDetectorScope.launch {
            try {
                var significantMotionStarted = false

                // 1) Attempt to start the significant-motion sensor
                if (significantMotionSensor.doesSensorExist()) {
                    significantMotionStarted = significantMotionSensor.startListeningToSensor()
                    if (significantMotionStarted) {
                        significantMotionSensor.setOnTriggerListener {
                            // This fires once per motion event.
                            // Very low power, but cannot be unregistered in some implementations.
                            handleMotionDetected()
                        }
                    }
                }

                // 2) If significantMotion not available or failed, start fallback right away.
                //    Otherwise, start a timer to see if significantMotion triggers soon.
                if (!significantMotionStarted) {
                    startFallbackDetection()
                } else {
                    setupFallbackTimer()
                }
            } catch (e: Exception) {
                Log.e("MotionDetection", "Error initializing hybrid motion detection", e)
                // If we fail for any reason, fallback to normal detection
                startFallbackDetection()
            }
        }
    }

    /**
     * Stops all motion detection, cancels coroutines, and unregisters sensors.
     */
    fun stopHybridMotionDetection() {
        motionDetectorScope.coroutineContext.cancelChildren()

        // Stop any hardware sensors. Use try-catch for safety.
        try {
            significantMotionSensor.stopListeningToSensor()
            linearAccelerationSensor.stopListeningToSensor()
            accelerometerSensor.stopListeningToSensor()
        } catch (e: Exception) {
            Log.e("MotionDetection", "Error stopping motion detection sensors", e)
        }
    }

    // --------------------
    //  HYBRID MOTION LOGIC
    // --------------------

    /**
     * Schedules a timer to see if significant-motion triggers within
     * SIGNIFICANT_MOTION_WAIT_DURATION. If not, we enable fallback detection.
     */
    private fun setupFallbackTimer() {
        fallbackJob?.cancel()
        fallbackJob = motionDetectorScope.launch {
            delay(SIGNIFICANT_MOTION_WAIT_DURATION)
            // If we haven't detected motion yet, fallback to normal sensors
            if (!isVehicleMoving) {
                startFallbackDetection()
            }
        }
    }

    /**
     * Enable fallback detection at SENSOR_DELAY_NORMAL, either linear-acceleration
     * or accelerometer if linear-acceleration is not available.
     */
    private fun startFallbackDetection() {
        isFallbackActive = true
        lastMotionTimestamp = System.currentTimeMillis()

        val identityMatrix = floatArrayOf(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f
        )

        motionDetectorScope.launch {
            try {
                if (linearAccelerationSensor.doesSensorExist()) {
                    linearAccelerationSensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
                    linearAccelerationSensor.whenSensorValueChangesListener { _, values, _ ->
                        val processed = processSensorData(
                            Sensor.TYPE_ACCELEROMETER,
                            values.toFloatArray(),
                            identityMatrix
                        )
                        checkMotion(processed.toList())
                    }
                } else if (accelerometerSensor.doesSensorExist()) {
                    accelerometerSensor.startListeningToSensor(SensorManager.SENSOR_DELAY_NORMAL)
                    accelerometerSensor.whenSensorValueChangesListener { _, values, _ ->
                        val processed = processSensorData(
                            Sensor.TYPE_ACCELEROMETER,
                            values.toFloatArray(),
                            identityMatrix
                        )
                        checkMotion(processed.toList())
                    }
                } else {
                    // If neither sensor is available, log and fallback is meaningless
                    Log.w("MotionDetection", "No linearAcceleration or accelerometer sensor available for fallback.")
                }
            } catch (e: Exception) {
                Log.e("MotionDetection", "Error in fallback motion detection", e)
            }
        }
    }

    // --------------------
    //  INACTIVITY & RESUME
    // --------------------

    /**
     * Pauses fallback sensors on inactivity to save battery.
     */
    private fun pauseFallbackDetection() {
        Log.d("MotionDetection", "Pausing fallback sensor listeners due to inactivity.")
        stopFallbackSensors()
        isFallbackActive = false
        resumeFallbackDetection()
    }

    /**
     * Resumes fallback detection after a RESUME_DELAY, if still inactive.
     */
    private fun resumeFallbackDetection() {
        motionDetectorScope.launch {
            delay(RESUME_DELAY)
            if (!isFallbackActive) {
                Log.d("MotionDetection", "Resuming fallback detection after inactivity.")
                startFallbackDetection()
            }
        }
    }

    /**
     * If no motion for INACTIVITY_TIMEOUT ms, we pause fallback detection.
     */
    private fun scheduleInactivityCheck() {
        inactivityCheckJob?.cancel()
        inactivityCheckJob = motionDetectorScope.launch {
            delay(INACTIVITY_TIMEOUT)
            val now = System.currentTimeMillis()
            if (now - lastMotionTimestamp >= INACTIVITY_TIMEOUT) {
                pauseFallbackDetection()
            }
        }
    }

    private fun stopFallbackSensors() {
        if (linearAccelerationSensor.doesSensorExist()) {
            linearAccelerationSensor.stopListeningToSensor()
        }
        if (accelerometerSensor.doesSensorExist()) {
            accelerometerSensor.stopListeningToSensor()
        }
    }

    // --------------------
    //  MOTION DETECTION
    // --------------------

    /**
     * Called on each fallback sensor reading. Throttles input to at most once
     * per MIN_DELAY_BETWEEN_PROCESSES ms, checks for motion, and triggers
     * inactivity scheduling.
     */
    private fun checkMotion(values: List<Float>) {
        val now = System.currentTimeMillis()

        // 1) Throttle events to reduce overhead
        if (now - lastProcessTime < MIN_DELAY_BETWEEN_PROCESSES) {
            return // Skip processing this time
        }
        lastProcessTime = now

        // 2) Compute magnitude
        val x = values.getOrNull(0) ?: 0f
        val y = values.getOrNull(1) ?: 0f
        val z = values.getOrNull(2) ?: 0f
        val magnitude = sqrt(x * x + y * y + z * z)

        // 3) Update a repository or UI. Because of throttling, this updates far less often.
        motionDetectorScope.launch {
            sensorDataColStateRepository.updateLinearAcceleration(magnitude)
        }

        // 4) Reset inactivity timer
        lastMotionTimestamp = now
        scheduleInactivityCheck()

        // 5) Debounce the final "motion detected" or "motion stopped" decision
        if (magnitude > LINEAR_ACCELERATION_THRESHOLD) {
            handleMotionDetected()
        } else {
            handleMotionStopped()
        }
    }

    /**
     * Called when motion is detected, with debouncing to avoid flaps.
     */
    private fun handleMotionDetected() {
        val now = System.currentTimeMillis()
        if (now - lastMotionChangeTimestamp > DEBOUNCE_INTERVAL) {
            lastMotionChangeTimestamp = now
            if (!isVehicleMoving) {
                isVehicleMoving = true
                Log.d("MotionDetection", "Motion detected: updating state to MOVING")
                notifyMotionDetected()
            }
        } else {
            // A new reading came in within the debounce window; schedule a confirm after DEBOUNCE_INTERVAL
            debounceJob?.cancel()
            debounceJob = motionDetectorScope.launch {
                delay(DEBOUNCE_INTERVAL)
                if (!isVehicleMoving) {
                    isVehicleMoving = true
                    Log.d("MotionDetection", "Debounce expired: confirming motion detected")
                    notifyMotionDetected()
                }
            }
        }
    }

    /**
     * Called when motion stops, with debouncing.
     */
    private fun handleMotionStopped() {
        val now = System.currentTimeMillis()
        if (now - lastMotionChangeTimestamp > DEBOUNCE_INTERVAL) {
            lastMotionChangeTimestamp = now
            if (isVehicleMoving) {
                isVehicleMoving = false
                Log.d("MotionDetection", "Motion stopped: updating state to STATIONARY")
                notifyMotionStopped()
            }
        } else {
            debounceJob?.cancel()
            debounceJob = motionDetectorScope.launch {
                delay(DEBOUNCE_INTERVAL)
                if (isVehicleMoving) {
                    isVehicleMoving = false
                    Log.d("MotionDetection", "Debounce expired: confirming motion stopped")
                    notifyMotionStopped()
                }
            }
        }
    }

    // --------------------
    //  LISTENER NOTIFICATIONS
    // --------------------

    private fun notifyMotionDetected() {
        motionListeners.forEach { it.onMotionDetected() }
    }

    private fun notifyMotionStopped() {
        motionListeners.forEach { it.onMotionStopped() }
    }

    // --------------------
    //  SENSOR DATA PROCESSING
    // --------------------

    /**
     * Basic pass-through to your shared function that applies any calibration or transform.
     */
    private fun processSensorData(
        sensorType: Int,
        inputValues: FloatArray,
        rotationMatrix: FloatArray
    ): FloatArray {
        return ProcessSensorData.processSensorData(sensorType, inputValues, rotationMatrix)
    }
}