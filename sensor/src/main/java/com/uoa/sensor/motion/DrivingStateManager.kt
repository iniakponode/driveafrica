package com.uoa.sensor.motion

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.SystemClock
import android.util.Log
import com.uoa.sensor.repository.SensorDataColStateRepository
import com.uoa.sensor.hardware.MotionFFTClassifier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.Constants.Companion.TRIP_DETECTION_SENSITIVITY
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
class DrivingStateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sensorDataColStateRepository: SensorDataColStateRepository
) : SensorEventListener {

    companion object {
        private const val TAG = "DrivingStateManager"

        // Accelerometer sampling
        private const val ACCEL_SAMPLING_HZ = 15
        private const val ACCEL_WINDOW_SIZE_SEC = 1
        private const val SAMPLES_PER_WINDOW = ACCEL_SAMPLING_HZ * ACCEL_WINDOW_SIZE_SEC

        // Variance thresholds for vehicle motion (m/s²)
        private const val VARIANCE_MAX_VEHICLE = 1.5
        private const val VARIANCE_WALKING = 2.5

        // Speed thresholds (m/s) - REDUCED for better detection
        private const val SPEED_STOPPED_THRESHOLD = 1.39 // ~5 km/h (~3 mph)

        // Timing constants
        private const val GPS_FALSE_ALARM_TIMEOUT_MS = 30_000L
        private const val GPS_TIMEOUT_FOR_FALLBACK_MS = 5_000L // 5 seconds for computed speed fallback
        private const val WALKING_EXIT_DURATION_MS = 5_000L
        private const val PARKING_TIMEOUT_MS = 180_000L // 3 minutes
        private const val MIN_RECORDING_DURATION_MS = 60_000L

        // GPS verification hardening
        private const val VERIFYING_REQUIRED_GPS_UPDATES = 2
        private const val VERIFYING_MAX_LOCATION_AGE_MS = 30_000L
        private const val VERIFYING_MAX_ACCURACY_M = 50f
        private const val VERIFYING_MAX_SPEED_ACCURACY_MPS = 5.0f
        private const val VERIFYING_ACCEPTABLE_LOCATION_AGE_MS = 30_000L
        private const val VERIFYING_ACCEPTABLE_ACCURACY_M = 100f
        private const val VERIFYING_ACCEPTABLE_SPEED_ACCURACY_MPS = 7.5f
        private const val VERIFYING_STRONG_SPEED_MULTIPLIER = 1.5

        // Fallback confirmation (when GPS is stale)
        private const val VERIFYING_FALLBACK_CONFIRMATION_MS = 5_000L
    }

    private data class SensitivityConfig(
        val label: String,
        val varianceMinVehicle: Double,
        val speedVehicleThreshold: Double,
        val smoothMotionDurationMs: Long
    )

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

    private val _currentAccuracy = MutableStateFlow(0f)
    val currentAccuracy: StateFlow<Float> = _currentAccuracy.asStateFlow()

    private var callback: StateCallback? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var sensitivityConfig = loadSensitivityConfig()
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == TRIP_DETECTION_SENSITIVITY) {
            sensitivityConfig = loadSensitivityConfig()
            Log.i(TAG, "Trip detection sensitivity set to ${sensitivityConfig.label}")
        }
    }

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
    private val motionClassifier = MotionFFTClassifier(
        sampleRate = ACCEL_SAMPLING_HZ,
        bufferSize = 64
    )

    // Timing jobs
    private var smoothMotionJob: Job? = null
    private var falseAlarmJob: Job? = null
    private var walkingExitJob: Job? = null
    private var parkingTimerJob: Job? = null

    // State tracking
    private var smoothMotionStartTime = 0L
    private var highVarianceStartTime = 0L
    private var stoppedStartTime = 0L
    private var recordingStartTime = 0L
    private var verifyingGoodFixCount = 0
    private var fallbackAboveThresholdStartTime = 0L

    private val _currentTripId = MutableStateFlow<UUID?>(null)
    val currentTripId: StateFlow<UUID?> = _currentTripId.asStateFlow()

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    /**
     * Initialize the state manager with callback
     */
    fun initialize(stateCallback: StateCallback) {
        callback = stateCallback
        Log.d(TAG, "DrivingStateManager initialized")
    }

    /**
     * Start monitoring for vehicle motion using the accelerometer.
     */
    fun startMonitoring() {
        if (_currentState.value == DrivingState.IDLE) {
            Log.d(TAG, "Starting vehicle motion monitoring")
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accelSensor != null) {
                try {
                    sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to register accelerometer listener", e)
                }
            } else {
                Log.w(TAG, "Accelerometer sensor not available; motion detection disabled.")
            }
        }
    }

    /**
     * Stop all monitoring and cleanup.
     */
    fun stopMonitoring() {
        Log.d(TAG, "Stopping vehicle motion monitoring")
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        try {
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister accelerometer listener", e)
        }
        cancelAllJobs()
        accelWindow.clear()
        transitionTo(DrivingState.IDLE)
        callback?.requestGpsDisable()
    }

    fun startTrip(tripId: UUID) {
        _currentTripId.value = tripId
        startMonitoring()
    }

    fun stopTrip() {
        _currentTripId.value = null
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
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
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

        if (motionClassifier.addSample(dynamicAccel)) {
            val classification = motionClassifier.classify()
            val state = _currentState.value
            if (state == DrivingState.IDLE || state == DrivingState.VERIFYING) {
                scope.launch {
                    sensorDataColStateRepository.updateMovementType(classification.label)
                }
            }
        }

        scope.launch {
            sensorDataColStateRepository.updateLinearAcceleration(dynamicAccel)
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
        lastGpsUpdateTime = System.currentTimeMillis()
        isUsingComputedSpeed = false

        // Calculate speeds in different units for dashboard comparison
        val speedKmh = currentSpeed * 3.6
        val speedMph = currentSpeed * 2.23694

        // Update state flow for UI
        _currentSpeedMph.value = speedMph

        _currentAccuracy.value = location.accuracy

        // Log comprehensive GPS data for dashboard comparison
        Log.i(TAG, "═══════════════════════════════════════════════════════")
        Log.i(TAG, "📍 GPS UPDATE:")
        Log.i(TAG, "   Speed (m/s):  %.2f m/s".format(currentSpeed))
        Log.i(TAG, "   Speed (km/h): %.1f km/h".format(speedKmh))
        Log.i(TAG, "   Speed (mph):  %.1f mph ⬅ COMPARE WITH DASHBOARD".format(speedMph))
        Log.i(TAG, "   Accuracy:     %.1f meters".format(location.accuracy))
        Log.i(TAG, "   State:        ${_currentState.value}")
        val speedThreshold = speedVehicleThreshold()
        Log.i(TAG, "   Threshold:    > %.1f mph (%.1f km/h) to confirm vehicle".format(speedThreshold * 2.23694, speedThreshold * 3.6))
        Log.i(TAG, "═══════════════════════════════════════════════════════")

        when (_currentState.value) {
            DrivingState.IDLE -> {
                val strongSpeed = currentSpeed >= speedThreshold * VERIFYING_STRONG_SPEED_MULTIPLIER
                val gpsCandidate = (isGoodGpsFix(location) ||
                    (strongSpeed && isAcceptableGpsFix(location))) &&
                    currentSpeed >= speedThreshold &&
                    (!isWalkingMotion() || strongSpeed)
                if (gpsCandidate) {
                    if (!isVehicleMotion()) {
                        scope.launch { sensorDataColStateRepository.updateMovementType("vehicle") }
                    }
                    Log.i(TAG, "IDLE -> VERIFYING: GPS speed above threshold, verifying")
                    transitionTo(DrivingState.VERIFYING)
                    callback?.requestGpsEnable()
                    handleVerifyingGpsUpdate(location)
                }
            }
            DrivingState.VERIFYING -> handleVerifyingGpsUpdate(location)
            DrivingState.RECORDING -> handleRecordingGpsUpdate()
            DrivingState.POTENTIAL_STOP -> handlePotentialStopGpsUpdate()
        }
    }

    /**
     * Update fallback speed (computed/fused) when GPS is stale.
     */
    fun updateFallbackSpeed(speedMps: Double, isGpsStale: Boolean) {
        if (!isGpsStale) return
        if (System.currentTimeMillis() - lastGpsUpdateTime < GPS_TIMEOUT_FOR_FALLBACK_MS) return

        val speedThreshold = speedVehicleThreshold()
        computedSpeed = speedMps
        if (!isUsingComputedSpeed) {
            Log.i(TAG, "Using computed speed fallback for movement detection")
        }
        isUsingComputedSpeed = true
        currentSpeed = computedSpeed
        _currentSpeedMph.value = currentSpeed * 2.23694

        when (_currentState.value) {
            DrivingState.IDLE -> {
                val strongSpeed = currentSpeed >= speedThreshold * VERIFYING_STRONG_SPEED_MULTIPLIER
                if (currentSpeed >= speedThreshold && (!isWalkingMotion() || strongSpeed)) {
                    if (!isVehicleMotion()) {
                        scope.launch { sensorDataColStateRepository.updateMovementType("vehicle") }
                    }
                    Log.i(TAG, "IDLE -> VERIFYING: Fallback speed above threshold, verifying")
                    transitionTo(DrivingState.VERIFYING)
                    callback?.requestGpsEnable()
                    handleVerifyingFallbackSpeedUpdate()
                }
            }
            DrivingState.VERIFYING -> handleVerifyingFallbackSpeedUpdate()
            DrivingState.RECORDING -> handleRecordingGpsUpdate()
            DrivingState.POTENTIAL_STOP -> handlePotentialStopGpsUpdate()
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
            Log.i(TAG, "   Vehicle Range: %.2f - %.2f m/s²".format(varianceMinVehicle(), VARIANCE_MAX_VEHICLE))
            Log.i(TAG, "   Walking Threshold: > %.2f m/s²".format(VARIANCE_WALKING))
            Log.i(TAG, "   Classification: %s".format(
                when {
                    variance < varianceMinVehicle() -> "Too Smooth (Stationary)"
                    variance in varianceMinVehicle()..VARIANCE_MAX_VEHICLE -> "✅ VEHICLE MOTION DETECTED"
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

        val motionLabel = sensorDataColStateRepository.movementLabel.value
        val hasVehicleLabel = motionLabel == "vehicle"
        val motionCandidate =
            variance in varianceMinVehicle()..VARIANCE_MAX_VEHICLE || hasVehicleLabel
        if (isWalkingMotion()) {
            if (smoothMotionStartTime != 0L) {
                Log.v(TAG, "IDLE - Walking/running label detected, resetting timer")
                smoothMotionStartTime = 0L
            }
            return
        }

        if (variance > VARIANCE_WALKING) {
            if (smoothMotionStartTime != 0L) {
                Log.v(TAG, "IDLE - High variance (walking/running), resetting timer")
                smoothMotionStartTime = 0L
            }
            return
        }

        // Check if variance indicates motion worth verifying with GPS
        if (motionCandidate) {
            // Start or continue tracking sustained motion
            if (smoothMotionStartTime == 0L) {
                smoothMotionStartTime = System.currentTimeMillis()
                Log.d(TAG, "IDLE - Motion detected, starting timer")
            }

            val duration = System.currentTimeMillis() - smoothMotionStartTime
            if (duration >= smoothMotionDurationMs()) {
                val gpsCandidate = lastLocation?.let { isGoodGpsFix(it) } == true &&
                    currentSpeed >= speedVehicleThreshold()
                if (!isVehicleMotion() && gpsCandidate) {
                    scope.launch { sensorDataColStateRepository.updateMovementType("vehicle") }
                }
                if (isVehicleMotion() || gpsCandidate) {
                    // Sustained motion for 5 seconds -> transition to VERIFYING
                    Log.i(TAG, "IDLE -> VERIFYING: Sustained motion detected")
                    smoothMotionStartTime = 0L
                    transitionTo(DrivingState.VERIFYING)
                    callback?.requestGpsEnable()
                }
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
            Log.v(TAG, "IDLE - High variance (walking/running) detected, ignoring")
        }
    }

    /**
     * VERIFYING State: Use GPS speed to confirm vehicle motion
     */
    private fun processVerifyingState(dynamicAccel: Double) {
        if (!isVehicleMotion()) {
            if (hasStrongGpsCandidate()) {
                scope.launch { sensorDataColStateRepository.updateMovementType("vehicle") }
                return
            }
            Log.i(TAG, "VERIFYING -> IDLE: Motion label is not vehicle")
            transitionTo(DrivingState.IDLE)
            callback?.requestGpsDisable()
            return
        }
        // Handle case where GPS is slow to start up
        if (falseAlarmJob?.isActive == true) return
        falseAlarmJob = scope.launch {
            delay(GPS_FALSE_ALARM_TIMEOUT_MS)
            if (_currentState.value == DrivingState.VERIFYING) {
                Log.w(TAG, "VERIFYING -> IDLE: GPS timeout, no speed confirmation")
                transitionTo(DrivingState.IDLE)
                callback?.requestGpsDisable()
            }
        }
    }

    /**
     * RECORDING State: Monitor for stops or non-vehicle motion
     */
    private fun processRecordingState(dynamicAccel: Double) {
        if (accelWindow.size < SAMPLES_PER_WINDOW) return

        val variance = calculateVariance()
        val now = System.currentTimeMillis()
        val recordingDurationMs = if (recordingStartTime > 0L) {
            now - recordingStartTime
        } else {
            0L
        }

        // If high variance (walking), start exit timer
        if (variance > VARIANCE_WALKING &&
            currentSpeed < SPEED_STOPPED_THRESHOLD &&
            recordingDurationMs >= MIN_RECORDING_DURATION_MS
        ) {
            if (walkingExitJob == null || walkingExitJob?.isActive == false) {
                walkingExitJob = scope.launch {
                    delay(WALKING_EXIT_DURATION_MS)
                    Log.w(TAG, "RECORDING -> IDLE: Confirmed walking/running, stopping trip")
                    transitionTo(DrivingState.IDLE)
                }
            }
        } else {
            walkingExitJob?.cancel()
        }

        // If low variance and low speed, potential stop
        if (variance < varianceMinVehicle() && currentSpeed < SPEED_STOPPED_THRESHOLD) {
            Log.i(TAG, "RECORDING -> POTENTIAL_STOP: Low speed and variance")
            transitionTo(DrivingState.POTENTIAL_STOP)
        }
    }

    /**
     * POTENTIAL_STOP State: Determine if parked or brief stop
     */
    private fun processPotentialStopState() {
        if (parkingTimerJob?.isActive == true) return
        // Start a timer to check if we are parked
        parkingTimerJob = scope.launch {
            delay(PARKING_TIMEOUT_MS)
            if (_currentState.value == DrivingState.POTENTIAL_STOP) {
                Log.i(TAG, "POTENTIAL_STOP -> IDLE: Parked for 3 minutes, stopping trip")
                transitionTo(DrivingState.IDLE)
            }
        }
    }

    // =====================================================================
    // GPS Update Handlers (for each state)
    // =====================================================================

    private fun handleVerifyingGpsUpdate(location: Location) {
        val strongSpeed = currentSpeed >= speedVehicleThreshold() * VERIFYING_STRONG_SPEED_MULTIPLIER
        if (!isGoodGpsFix(location) && !(strongSpeed && isAcceptableGpsFix(location))) {
            verifyingGoodFixCount = 0
            return
        }
        if (currentSpeed >= speedVehicleThreshold()) {
            if (!isVehicleMotion()) {
                scope.launch { sensorDataColStateRepository.updateMovementType("vehicle") }
            }
            verifyingGoodFixCount++
            if (verifyingGoodFixCount >= VERIFYING_REQUIRED_GPS_UPDATES) {
                Log.i(TAG, "VERIFYING -> RECORDING: GPS speed confirmed in $verifyingGoodFixCount updates")
                falseAlarmJob?.cancel()
                transitionTo(DrivingState.RECORDING)
                callback?.onDriveStarted()
            }
        } else {
            verifyingGoodFixCount = 0
        }
    }

    private fun handleVerifyingFallbackSpeedUpdate() {
        if (!isVehicleMotion()) {
            fallbackAboveThresholdStartTime = 0L
            return
        }
        val now = System.currentTimeMillis()
        if (currentSpeed >= speedVehicleThreshold()) {
            if (fallbackAboveThresholdStartTime == 0L) {
                fallbackAboveThresholdStartTime = now
            }
            if (now - fallbackAboveThresholdStartTime >= VERIFYING_FALLBACK_CONFIRMATION_MS) {
                Log.i(TAG, "VERIFYING -> RECORDING: Fallback speed sustained above threshold")
                falseAlarmJob?.cancel()
                transitionTo(DrivingState.RECORDING)
                callback?.onDriveStarted()
            }
        } else {
            fallbackAboveThresholdStartTime = 0L
        }
    }

    private fun handleRecordingGpsUpdate() {
        if (currentSpeed < SPEED_STOPPED_THRESHOLD) {
            Log.i(TAG, "RECORDING -> POTENTIAL_STOP: GPS speed dropped below threshold")
            transitionTo(DrivingState.POTENTIAL_STOP)
        }
    }

    private fun handlePotentialStopGpsUpdate() {
        if (currentSpeed >= speedVehicleThreshold()) {
            Log.i(TAG, "POTENTIAL_STOP -> RECORDING: GPS speed recovered, resuming trip")
            parkingTimerJob?.cancel()
            transitionTo(DrivingState.RECORDING)
        }
    }

    // =====================================================================
    // Utility Functions
    // =====================================================================

    private fun loadSensitivityConfig(): SensitivityConfig {
        val value = prefs.getString(TRIP_DETECTION_SENSITIVITY, "balanced")
        return when (value) {
            "high" -> SensitivityConfig(
                label = "high",
                varianceMinVehicle = 0.03,
                speedVehicleThreshold = 1.5,
                smoothMotionDurationMs = 1_500L
            )
            "low" -> SensitivityConfig(
                label = "low",
                varianceMinVehicle = 0.08,
                speedVehicleThreshold = 4.0,
                smoothMotionDurationMs = 3_000L
            )
            else -> SensitivityConfig(
                label = "balanced",
                varianceMinVehicle = 0.05,
                speedVehicleThreshold = 2.5,
                smoothMotionDurationMs = 2_000L
            )
        }
    }

    private fun varianceMinVehicle(): Double = sensitivityConfig.varianceMinVehicle

    private fun speedVehicleThreshold(): Double = sensitivityConfig.speedVehicleThreshold

    private fun smoothMotionDurationMs(): Long = sensitivityConfig.smoothMotionDurationMs

    private fun transitionTo(newState: DrivingState) {
        val previousState = _currentState.value
        if (_currentState.value == newState) return
        val now = System.currentTimeMillis()
        _currentState.value = newState
        callback?.onStateChanged(newState)
        Log.d(TAG, "State changed to: $newState")
        if (newState == DrivingState.IDLE &&
            (previousState == DrivingState.RECORDING ||
                previousState == DrivingState.POTENTIAL_STOP ||
                previousState == DrivingState.VERIFYING)
        ) {
            callback?.onDriveStopped()
        }
        if (newState == DrivingState.RECORDING && recordingStartTime == 0L) {
            recordingStartTime = now
        } else if (newState == DrivingState.IDLE) {
            recordingStartTime = 0L
        }

        // Reset timers on state change
        cancelAllJobs()
        smoothMotionStartTime = 0L
        highVarianceStartTime = 0L
        stoppedStartTime = 0L
        verifyingGoodFixCount = 0
        fallbackAboveThresholdStartTime = 0L
    }

    private fun cancelAllJobs() {
        smoothMotionJob?.cancel()
        falseAlarmJob?.cancel()
        walkingExitJob?.cancel()
        parkingTimerJob?.cancel()
    }

    private fun calculateVariance(): Double {
        if (accelWindow.isEmpty()) return 0.0

        val mean = accelWindow.average()
        val variance = accelWindow.map { (it - mean).pow(2) }.average()
        _currentVariance.value = variance
        return variance
    }

    private fun isVehicleMotion(): Boolean {
        val label = sensorDataColStateRepository.movementLabel.value
        if (label == "walking" || label == "running") return false
        if (_currentState.value == DrivingState.VERIFYING) return true
        return label == "vehicle" || label == "verifying"
    }

    private fun isWalkingMotion(): Boolean {
        return when (sensorDataColStateRepository.movementLabel.value) {
            "walking", "running" -> true
            else -> false
        }
    }

    private fun isGoodGpsFix(location: Location): Boolean {
        val nowNanos = SystemClock.elapsedRealtimeNanos()
        val ageMs = if (location.elapsedRealtimeNanos > 0) {
            (nowNanos - location.elapsedRealtimeNanos) / 1_000_000
        } else {
            Long.MAX_VALUE
        }
        if (ageMs > VERIFYING_MAX_LOCATION_AGE_MS) return false
        if (location.hasAccuracy() && location.accuracy > VERIFYING_MAX_ACCURACY_M) return false
        if (location.hasSpeedAccuracy() &&
            location.speedAccuracyMetersPerSecond > VERIFYING_MAX_SPEED_ACCURACY_MPS
        ) return false
        return true
    }

    private fun isAcceptableGpsFix(location: Location): Boolean {
        val nowNanos = SystemClock.elapsedRealtimeNanos()
        val ageMs = if (location.elapsedRealtimeNanos > 0) {
            (nowNanos - location.elapsedRealtimeNanos) / 1_000_000
        } else {
            Long.MAX_VALUE
        }
        if (ageMs > VERIFYING_ACCEPTABLE_LOCATION_AGE_MS) return false
        if (location.hasAccuracy() && location.accuracy > VERIFYING_ACCEPTABLE_ACCURACY_M) return false
        if (location.hasSpeedAccuracy() &&
            location.speedAccuracyMetersPerSecond > VERIFYING_ACCEPTABLE_SPEED_ACCURACY_MPS
        ) return false
        return true
    }

    private fun hasStrongGpsCandidate(): Boolean {
        val location = lastLocation ?: return false
        if (currentSpeed < speedVehicleThreshold()) return false
        val strongSpeed = currentSpeed >= speedVehicleThreshold() * VERIFYING_STRONG_SPEED_MULTIPLIER
        return isGoodGpsFix(location) || (strongSpeed && isAcceptableGpsFix(location))
    }
}
