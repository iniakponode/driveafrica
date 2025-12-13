package com.uoa.sensor.motion

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * DrivingStateManager - A Finite State Machine for Smart Motion Trigger
 *
 * Implements battery-efficient vehicle detection using:
 * - Accelerometer for low-power motion monitoring
 * - GPS for ground-truth speed verification
 *
 * States:
 * - IDLE: Low-power monitoring with accelerometer only
 * - VERIFYING: GPS enabled to verify vehicle motion
 * - RECORDING: Active data collection during drive
 * - POTENTIAL_STOP: Detecting if vehicle is parked vs traffic light
 */
@Singleton
class DrivingStateManager @Inject constructor() : SensorEventListener {

    companion object {
        private const val TAG = "DrivingStateManager"

        // Accelerometer sampling
        private const val ACCEL_SAMPLING_HZ = 15
        private const val ACCEL_WINDOW_SIZE_SEC = 1
        private const val SAMPLES_PER_WINDOW = ACCEL_SAMPLING_HZ * ACCEL_WINDOW_SIZE_SEC

        // Variance thresholds for vehicle motion (m/s²)
        private const val VARIANCE_MIN_VEHICLE = 0.15
        private const val VARIANCE_MAX_VEHICLE = 1.5
        private const val VARIANCE_WALKING = 2.5

        // Speed thresholds (m/s) - REDUCED for better detection
        private const val SPEED_VEHICLE_THRESHOLD = 4.0 // ~14.4 km/h (~9 mph) - REDUCED from 15 km/h
        private const val SPEED_STOPPED_THRESHOLD = 1.39 // ~5 km/h (~3 mph)

        // Timing constants
        private const val SMOOTH_MOTION_DURATION_MS = 5_000L
        private const val GPS_FALSE_ALARM_TIMEOUT_MS = 30_000L
        private const val GPS_TIMEOUT_FOR_FALLBACK_MS = 5_000L // 5 seconds for computed speed fallback
        private const val WALKING_EXIT_DURATION_MS = 5_000L
        private const val PARKING_TIMEOUT_MS = 180_000L // 3 minutes
    }

    /**
     * Driving detection states
     */
    enum class DrivingState {
        IDLE,           // Low power mode, accelerometer only
        VERIFYING,      // GPS check in progress
        RECORDING,      // Active drive recording
        POTENTIAL_STOP  // Vehicle may be parked or at traffic light
    }

    /**
     * Callback interface for state manager events
     */
    interface StateCallback {
        fun onDriveStarted()
        fun onDriveStopped()
        fun requestGpsEnable()
        fun requestGpsDisable()
        fun onStateChanged(newState: DrivingState)
    }

    // State management
    private val _currentState = MutableStateFlow(DrivingState.IDLE)
    val currentState: StateFlow<DrivingState> = _currentState.asStateFlow()

    // Expose variance for UI
    private val _currentVariance = MutableStateFlow(0.0)
    val currentVariance: StateFlow<Double> = _currentVariance.asStateFlow()

    // Expose current speed for UI
    private val _currentSpeedMph = MutableStateFlow(0.0)
    val currentSpeedMph: StateFlow<Double> = _currentSpeedMph.asStateFlow()

    private var callback: StateCallback? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // UI update callback (optional)
    private var uiUpdateCallback: ((variance: Double, speedMph: Double, accuracy: Float) -> Unit)? = null

    // Accelerometer data
    private val accelWindow = ConcurrentLinkedQueue<Double>()
    private var lastAccelProcessTime = 0L
    private val accelProcessInterval = 1000L / ACCEL_SAMPLING_HZ

    // GPS data
    private var lastLocation: Location? = null
    private var currentSpeed = 0.0 // m/s
    private var lastGpsUpdateTime = 0L
    private var computedSpeed = 0.0 // m/s - computed from accelerometer as fallback
    private var isUsingComputedSpeed = false

    // Timing jobs
    private var smoothMotionJob: Job? = null
    private var falseAlarmJob: Job? = null
    private var walkingExitJob: Job? = null
    private var parkingTimerJob: Job? = null

    // State tracking
    private var smoothMotionStartTime = 0L
    private var highVarianceStartTime = 0L
    private var stoppedStartTime = 0L

    /**
     * Initialize the state manager with callback
     */
    fun initialize(stateCallback: StateCallback) {
        callback = stateCallback
        Log.d(TAG, "DrivingStateManager initialized")
    }

    /**
     * Set UI update callback for real-time monitoring screen
     */
    fun setUiUpdateCallback(callback: (variance: Double, speedMph: Double, accuracy: Float) -> Unit) {
        uiUpdateCallback = callback
    }

    /**
     * Start monitoring for vehicle motion
     */
    fun startMonitoring() {
        if (_currentState.value == DrivingState.IDLE) {
            Log.d(TAG, "Starting vehicle motion monitoring")
            // Callback should register this as SensorEventListener
            // GPS remains disabled in IDLE state
        }
    }

    /**
     * Stop all monitoring and cleanup
     */
    fun stopMonitoring() {
        Log.d(TAG, "Stopping vehicle motion monitoring")
        cancelAllJobs()
        accelWindow.clear()
        transitionTo(DrivingState.IDLE)
        callback?.requestGpsDisable()
    }

    /**
     * Release resources
     */
    fun release() {
        stopMonitoring()
        scope.cancel()
        callback = null
    }

    // =====================================================================
    // SensorEventListener Implementation
    // =====================================================================

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val now = System.currentTimeMillis()
        if (now - lastAccelProcessTime < accelProcessInterval) return
        lastAccelProcessTime = now

        // Calculate magnitude (remove gravity component)
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Magnitude of acceleration vector
        val magnitude = sqrt(x.pow(2) + y.pow(2) + z.pow(2).toDouble())

        // Remove gravity (9.8 m/s²) to get dynamic acceleration
        val dynamicAccel = kotlin.math.abs(magnitude - 9.8)

        // Add to sliding window
        accelWindow.offer(dynamicAccel)
        if (accelWindow.size > SAMPLES_PER_WINDOW) {
            accelWindow.poll()
        }

        // Process based on current state
        when (_currentState.value) {
            DrivingState.IDLE -> processIdleState()
            DrivingState.VERIFYING -> processVerifyingState(dynamicAccel)
            DrivingState.RECORDING -> processRecordingState(dynamicAccel)
            DrivingState.POTENTIAL_STOP -> processPotentialStopState()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this use case
    }

    // =====================================================================
    // Location Updates (called externally when GPS is enabled)
    // =====================================================================

    /**
     * Update current location and speed
     * Should be called by parent service when GPS provides updates
     */
    fun updateLocation(location: Location) {
        lastLocation = location
        currentSpeed = location.speed.toDouble() // m/s

        // Calculate speeds in different units for dashboard comparison
        val speedKmh = currentSpeed * 3.6
        val speedMph = currentSpeed * 2.23694

        // Update state flow for UI
        _currentSpeedMph.value = speedMph

        // Call UI update callback with latest data
        uiUpdateCallback?.invoke(_currentVariance.value, speedMph, location.accuracy)

        // Log comprehensive GPS data for dashboard comparison
        Log.i(TAG, "═══════════════════════════════════════════════════════")
        Log.i(TAG, "📍 GPS UPDATE:")
        Log.i(TAG, "   Speed (m/s):  %.2f m/s".format(currentSpeed))
        Log.i(TAG, "   Speed (km/h): %.1f km/h".format(speedKmh))
        Log.i(TAG, "   Speed (mph):  %.1f mph ⬅ COMPARE WITH DASHBOARD".format(speedMph))
        Log.i(TAG, "   Accuracy:     %.1f meters".format(location.accuracy))
        Log.i(TAG, "   State:        ${_currentState.value}")
        Log.i(TAG, "   Threshold:    > %.1f mph (%.1f km/h) to confirm vehicle".format(SPEED_VEHICLE_THRESHOLD * 2.23694, SPEED_VEHICLE_THRESHOLD * 3.6))
        Log.i(TAG, "═══════════════════════════════════════════════════════")

        when (_currentState.value) {
            DrivingState.VERIFYING -> handleVerifyingGpsUpdate()
            DrivingState.RECORDING -> handleRecordingGpsUpdate()
            DrivingState.POTENTIAL_STOP -> handlePotentialStopGpsUpdate()
            else -> {}
        }
    }

    // =====================================================================
    // State Processing Logic
    // =====================================================================

    /**
     * IDLE State: Monitor for smooth sustained motion
     */
    private fun processIdleState() {
        if (accelWindow.size < SAMPLES_PER_WINDOW) return

        val variance = calculateVariance()

        // Detailed logging every 2 seconds to avoid spam
        val now = System.currentTimeMillis()
        if (now - lastAccelProcessTime >= 2000) {
            Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.i(TAG, "🔍 MOTION ANALYSIS (IDLE State):")
            Log.i(TAG, "   Variance:     %.3f m/s²".format(variance))
            Log.i(TAG, "   Vehicle Range: %.2f - %.2f m/s²".format(VARIANCE_MIN_VEHICLE, VARIANCE_MAX_VEHICLE))
            Log.i(TAG, "   Walking Threshold: > %.2f m/s²".format(VARIANCE_WALKING))
            Log.i(TAG, "   Classification: %s".format(
                when {
                    variance < VARIANCE_MIN_VEHICLE -> "Too Smooth (Stationary)"
                    variance in VARIANCE_MIN_VEHICLE..VARIANCE_MAX_VEHICLE -> "✅ VEHICLE MOTION DETECTED"
                    variance > VARIANCE_WALKING -> "Walking/Running (Filtered)"
                    else -> "Unknown"
                }
            ))
            if (smoothMotionStartTime != 0L) {
                val duration = (now - smoothMotionStartTime) / 1000.0
                Log.i(TAG, "   Timer: %.1f / 5.0 seconds".format(duration))
            }
            Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }

        // Check if variance indicates vehicle motion
        if (variance in VARIANCE_MIN_VEHICLE..VARIANCE_MAX_VEHICLE) {
            // Start or continue tracking smooth motion
            if (smoothMotionStartTime == 0L) {
                smoothMotionStartTime = System.currentTimeMillis()
                Log.d(TAG, "IDLE - Smooth motion detected, starting timer")
            }

            val duration = System.currentTimeMillis() - smoothMotionStartTime
            if (duration >= SMOOTH_MOTION_DURATION_MS) {
                // Sustained smooth motion for 5 seconds -> transition to VERIFYING
                Log.i(TAG, "IDLE -> VERIFYING: Sustained smooth motion detected")
                smoothMotionStartTime = 0L
                transitionTo(DrivingState.VERIFYING)
                callback?.requestGpsEnable()
            }
        } else {
            // Reset smooth motion timer if variance is out of range
            if (smoothMotionStartTime != 0L) {
                Log.v(TAG, "IDLE - Smooth motion interrupted, resetting timer")
                smoothMotionStartTime = 0L
            }
        }

        // Check for high variance (walking/running) - should NOT trigger
        if (variance > VARIANCE_WALKING) {
            Log.v(TAG, "IDLE - High variance detected (walking/running), ignoring")
            smoothMotionStartTime = 0L
        }
    }

    /**
     * VERIFYING State: GPS check to confirm vehicle motion
     */
    private fun processVerifyingState(dynamicAccel: Double) {
        // Check for walking exit
        if (dynamicAccel > VARIANCE_WALKING) {
            if (highVarianceStartTime == 0L) {
                highVarianceStartTime = System.currentTimeMillis()
            }

            val duration = System.currentTimeMillis() - highVarianceStartTime
            if (duration >= WALKING_EXIT_DURATION_MS) {
                Log.i(TAG, "VERIFYING -> IDLE: Walking detected")
                transitionTo(DrivingState.IDLE)
                callback?.requestGpsDisable()
                highVarianceStartTime = 0L
                return
            }
        } else {
            highVarianceStartTime = 0L
        }

        // Check if GPS has timed out (no GPS update for 5 seconds)
        val now = System.currentTimeMillis()
        val timeSinceLastGpsUpdate = now - lastGpsUpdateTime

        if (timeSinceLastGpsUpdate >= GPS_TIMEOUT_FOR_FALLBACK_MS && lastGpsUpdateTime > 0) {
            // GPS timeout - fall back to computed speed from accelerometer variance
            Log.w(TAG, "GPS timeout after 5 seconds, falling back to computed speed")

            val variance = calculateVariance()

            // Use variance as a proxy for motion - if we have sustained vehicle variance, assume moving
            if (variance in VARIANCE_MIN_VEHICLE..VARIANCE_MAX_VEHICLE) {
                Log.i(TAG, "")
                Log.i(TAG, "✅ VEHICLE CONFIRMED (Computed from sensors - GPS unavailable)")
                Log.i(TAG, "   Variance: %.3f m/s² (Vehicle range)".format(variance))
                Log.i(TAG, "   ➡️  VERIFYING -> RECORDING")
                Log.i(TAG, "   🚗 Starting trip automatically...")
                Log.i(TAG, "")

                cancelJob(falseAlarmJob)
                transitionTo(DrivingState.RECORDING)
                callback?.onDriveStarted()
                isUsingComputedSpeed = true
                return
            } else {
                Log.d(TAG, "GPS timeout but variance (%.3f) not in vehicle range, reverting to IDLE".format(variance))
                transitionTo(DrivingState.IDLE)
                callback?.requestGpsDisable()
                return
            }
        }

        // Start false alarm timer when entering VERIFYING
        if (falseAlarmJob == null) {
            falseAlarmJob = scope.launch {
                delay(GPS_FALSE_ALARM_TIMEOUT_MS)
                if (_currentState.value == DrivingState.VERIFYING && currentSpeed < SPEED_VEHICLE_THRESHOLD) {
                    Log.i(TAG, "VERIFYING -> IDLE: False alarm timeout")
                    transitionTo(DrivingState.IDLE)
                    callback?.requestGpsDisable()
                }
            }
        }
    }

    /**
     * Handle GPS updates in VERIFYING state
     */
    private fun handleVerifyingGpsUpdate() {
        val speedKmh = currentSpeed * 3.6
        val speedMph = currentSpeed * 2.23694

        if (currentSpeed >= SPEED_VEHICLE_THRESHOLD) {
            // Speed confirms vehicle motion -> start recording
            Log.i(TAG, "")
            Log.i(TAG, "✅ VEHICLE CONFIRMED!")
            Log.i(TAG, "   Current Speed: %.1f mph (%.1f km/h)".format(speedMph, speedKmh))
            Log.i(TAG, "   Threshold: > %.1f mph".format(SPEED_VEHICLE_THRESHOLD * 2.23694))
            Log.i(TAG, "   ➡️  VERIFYING -> RECORDING")
            Log.i(TAG, "   🚗 Starting trip automatically...")
            Log.i(TAG, "")

            cancelJob(falseAlarmJob)
            transitionTo(DrivingState.RECORDING)
            callback?.onDriveStarted()
        } else if (currentSpeed < SPEED_STOPPED_THRESHOLD) {
            // Low speed maintained - check if false alarm timeout is close
            Log.d(TAG, "VERIFYING: Low speed (%.1f mph / %.1f km/h), waiting for confirmation or timeout".format(speedMph, speedKmh))
        } else {
            // Speed is between stopped and vehicle threshold
            Log.d(TAG, "VERIFYING: Speed (%.1f mph) close to threshold (%.1f mph), monitoring...".format(speedMph, SPEED_VEHICLE_THRESHOLD * 2.23694))
        }
    }

    /**
     * RECORDING State: Active data collection
     */
    private fun processRecordingState(dynamicAccel: Double) {
        // Check for walking exit
        if (dynamicAccel > VARIANCE_WALKING) {
            if (highVarianceStartTime == 0L) {
                highVarianceStartTime = System.currentTimeMillis()
                Log.d(TAG, "RECORDING: High variance detected, starting walking timer")
            }

            val duration = System.currentTimeMillis() - highVarianceStartTime
            if (duration >= WALKING_EXIT_DURATION_MS) {
                Log.i(TAG, "RECORDING -> IDLE: User walked away")
                stopDriveAndTransitionToIdle()
                return
            }
        } else {
            if (highVarianceStartTime != 0L) {
                Log.v(TAG, "RECORDING: High variance ended, resetting walking timer")
            }
            highVarianceStartTime = 0L
        }
    }

    /**
     * Handle GPS updates in RECORDING state
     */
    private fun handleRecordingGpsUpdate() {
        val speedKmh = currentSpeed * 3.6
        val speedMph = currentSpeed * 2.23694

        if (currentSpeed < SPEED_STOPPED_THRESHOLD) {
            // Vehicle slowing down or stopped
            if (stoppedStartTime == 0L) {
                stoppedStartTime = System.currentTimeMillis()
                Log.i(TAG, "")
                Log.i(TAG, "⚠️ VEHICLE SLOWING/STOPPED")
                Log.i(TAG, "   Speed: %.1f mph (%.1f km/h)".format(speedMph, speedKmh))
                Log.i(TAG, "   Below threshold: < %.1f mph".format(SPEED_STOPPED_THRESHOLD * 2.23694))
                Log.i(TAG, "   ➡️  RECORDING -> POTENTIAL_STOP")
                Log.i(TAG, "   ⏱  Starting 3-minute parking timer...")
                Log.i(TAG, "")

                transitionTo(DrivingState.POTENTIAL_STOP)
                startParkingTimer()
            }
        } else {
            // Vehicle moving normally, reset any potential stop tracking
            if (stoppedStartTime != 0L) {
                Log.i(TAG, "RECORDING: Speed resumed (%.1f mph), canceling stop timer".format(speedMph))
                stoppedStartTime = 0L
            }
        }
    }

    /**
     * POTENTIAL_STOP State: Determine if parked or at traffic light
     */
    private fun processPotentialStopState() {
        // Monitoring continues, main logic is in GPS updates and parking timer
    }

    /**
     * Handle GPS updates in POTENTIAL_STOP state
     */
    private fun handlePotentialStopGpsUpdate() {
        val speedKmh = currentSpeed * 3.6
        val speedMph = currentSpeed * 2.23694

        if (currentSpeed >= SPEED_VEHICLE_THRESHOLD) {
            // Vehicle resumed motion -> back to RECORDING
            Log.i(TAG, "")
            Log.i(TAG, "🚦 MOTION RESUMED (Traffic light/Stop sign)")
            Log.i(TAG, "   Speed: %.1f mph (%.1f km/h)".format(speedMph, speedKmh))
            Log.i(TAG, "   Above threshold: > %.1f mph".format(SPEED_VEHICLE_THRESHOLD * 2.23694))
            Log.i(TAG, "   ➡️  POTENTIAL_STOP -> RECORDING")
            Log.i(TAG, "   🚗 Continuing trip...")
            Log.i(TAG, "")

            cancelJob(parkingTimerJob)
            stoppedStartTime = 0L
            transitionTo(DrivingState.RECORDING)
        } else {
            // Log parking timer countdown
            if (stoppedStartTime != 0L) {
                val elapsed = System.currentTimeMillis() - stoppedStartTime
                val remaining = (PARKING_TIMEOUT_MS - elapsed) / 1000
                if (remaining > 0 && remaining % 30 == 0L) { // Log every 30 seconds
                    Log.i(TAG, "POTENTIAL_STOP: Speed %.1f mph, parking timer: %d seconds remaining".format(speedMph, remaining))
                }
            }
        }
        // If speed remains low, parking timer will eventually trigger
    }

    /**
     * Start parking timer (3 minutes)
     */
    private fun startParkingTimer() {
        cancelJob(parkingTimerJob)
        parkingTimerJob = scope.launch {
            delay(PARKING_TIMEOUT_MS)
            if (_currentState.value == DrivingState.POTENTIAL_STOP) {
                Log.i(TAG, "POTENTIAL_STOP -> IDLE: Vehicle parked (3 min timeout)")
                stopDriveAndTransitionToIdle()
            }
        }
    }

    // =====================================================================
    // Utility Methods
    // =====================================================================

    /**
     * Calculate variance of acceleration magnitude
     */
    private fun calculateVariance(): Double {
        if (accelWindow.isEmpty()) return 0.0

        val values = accelWindow.toList()
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()

        // Update state flow for UI
        _currentVariance.value = variance

        return variance
    }

    /**
     * Transition to new state
     */
    private fun transitionTo(newState: DrivingState) {
        val oldState = _currentState.value
        if (oldState == newState) return

        Log.i(TAG, "State Transition: $oldState -> $newState")
        _currentState.value = newState

        // Reset GPS update timestamp when entering VERIFYING to start timeout counter
        if (newState == DrivingState.VERIFYING) {
            lastGpsUpdateTime = System.currentTimeMillis()
        }

        callback?.onStateChanged(newState)
    }

    /**
     * Stop drive and transition to IDLE
     */
    private fun stopDriveAndTransitionToIdle() {
        callback?.onDriveStopped()
        callback?.requestGpsDisable()
        cancelAllJobs()
        stoppedStartTime = 0L
        highVarianceStartTime = 0L
        smoothMotionStartTime = 0L
        accelWindow.clear()
        transitionTo(DrivingState.IDLE)
    }

    /**
     * Cancel all timing jobs
     */
    private fun cancelAllJobs() {
        cancelJob(smoothMotionJob)
        cancelJob(falseAlarmJob)
        cancelJob(walkingExitJob)
        cancelJob(parkingTimerJob)
    }

    /**
     * Cancel a specific job
     */
    private fun cancelJob(job: Job?) {
        job?.cancel()
    }
}

