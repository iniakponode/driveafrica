package com.uoa.sensor.presentation.viewModel

import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.sensor.hardware.HardwareModule
import com.uoa.sensor.repository.SensorDataColStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.parcelize.Parcelize

@Parcelize
data class VehicleDetectionUiState(
    // Current state
    val currentState: String = "IDLE",

    // GPS Speed data
    val speedMs: Double = 0.0,
    val speedKmh: Double = 0.0,
    val speedMph: Double = 0.0,
    val accuracy: Float = 0f,

    // Computed speed from sensors (fallback)
    val computedSpeedMph: Double = 0.0,
    val isUsingComputedSpeed: Boolean = false,
    val gpsTimeout: Boolean = false,

    // Speed thresholds
    val speedThresholdMph: Double = 9.0,
    val stoppedThresholdMph: Double = 3.1,

    // Motion analysis
    val variance: Double = 0.0,
    val classification: String = "Unknown",
    val timerProgress: Float = 0f,

    // Variance thresholds
    val varianceMin: Double = 0.15,
    val varianceMax: Double = 1.5,
    val walkingThreshold: Double = 2.5,

    // Trip info
    val isRecording: Boolean = false,
    val tripDuration: String = "00:00:00",
    val tripId: String = "",

    // Last update timestamp for debugging
    val lastUpdate: Long = 0L
) : Parcelable

@HiltViewModel
class VehicleDetectionViewModel @Inject constructor(
    private val sensorDataColStateRepository: SensorDataColStateRepository,
    private val hardwareModule: com.uoa.sensor.hardware.HardwareModule,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "VehicleDetectionVM"
        private const val KEY_UI_STATE = "vehicle_detection_ui_state"
        private const val KEY_TRIP_START_TIME = "trip_start_time"
        private const val GPS_TIMEOUT_MS = 5000L // 5 seconds
    }

    // Make the UI state survive process death and configuration changes.
    val uiState: StateFlow<VehicleDetectionUiState> = savedStateHandle.getStateFlow(KEY_UI_STATE, VehicleDetectionUiState())

    // Trip start time for duration calculation - saved across config changes
    private var tripStartTime: Long
        get() = savedStateHandle.get<Long>(KEY_TRIP_START_TIME) ?: 0L
        set(value) = savedStateHandle.set(KEY_TRIP_START_TIME, value)

    // Get DrivingStateManager for real-time data
    private val drivingStateManager = hardwareModule.getDrivingStateManager()

    // Track last GPS update time
    private var lastGpsUpdateTime = 0L

    init {
        Log.d(TAG, "ViewModel initialized")
        observeSensorState()
        observeDrivingStateManager()
        setupUiCallback()

        // Restore trip duration if was recording
        if (tripStartTime != 0L) {
            startDurationTimer()
        }
    }

    private fun observeDrivingStateManager() {
        // Combine all flows for efficient updates
        viewModelScope.launch {
            combine(
                drivingStateManager.currentState,
                drivingStateManager.currentVariance,
                drivingStateManager.currentSpeedMph
            ) { state, variance, speedMph ->
                Triple(state, variance, speedMph)
            }.collect { (state, variance, speedMph) ->
                val now = System.currentTimeMillis()
                val speedMs = speedMph / 2.23694

                // Check for GPS timeout
                val gpsTimeout = (now - lastGpsUpdateTime) > GPS_TIMEOUT_MS &&
                                 state.name == "VERIFYING"

                savedStateHandle[KEY_UI_STATE] = uiState.value.copy(
                    currentState = state.name,
                    variance = variance,
                    speedMs = speedMs,
                    speedKmh = speedMs * 3.6,
                    speedMph = speedMph,
                    gpsTimeout = gpsTimeout,
                    lastUpdate = now
                )

                // Update classification based on variance
                updateClassification(variance)

                Log.v(TAG, "State update: ${state.name}, Speed: $speedMph mph, Variance: $variance")
            }
        }
    }

    private fun setupUiCallback() {
        drivingStateManager.setUiUpdateCallback { variance, speedMph, accuracy ->
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                lastGpsUpdateTime = now

                savedStateHandle[KEY_UI_STATE] = uiState.value.copy(
                    variance = variance,
                    accuracy = accuracy,
                    speedMs = speedMph / 2.23694,
                    speedKmh = (speedMph / 2.23694) * 3.6,
                    speedMph = speedMph,
                    gpsTimeout = false,
                    lastUpdate = now
                )

                Log.v(TAG, "GPS update: $speedMph mph, accuracy: $accuracy m")
            }
        }
    }

    private fun observeSensorState() {
        // Observe collection status
        viewModelScope.launch {
            sensorDataColStateRepository.collectionStatus.collect { isCollecting ->
                savedStateHandle[KEY_UI_STATE] = uiState.value.copy(isRecording = isCollecting)

                // Handle trip timing
                if (isCollecting && tripStartTime == 0L) {
                    tripStartTime = System.currentTimeMillis()
                    startDurationTimer()
                    Log.d(TAG, "Trip started")
                } else if (!isCollecting && tripStartTime != 0L) {
                    tripStartTime = 0L
                    Log.d(TAG, "Trip ended")
                }
            }
        }

        // Observe trip ID from HardwareModule
        viewModelScope.launch {
            hardwareModule.currentTripIdFlow().collect { tripId ->
                savedStateHandle[KEY_UI_STATE] = uiState.value.copy(tripId = tripId?.toString() ?: "")
                Log.d(TAG, "Trip ID updated: $tripId")
            }
        }

        // Observe linear acceleration for computed speed (fallback)
        viewModelScope.launch {
            snapshotFlow { sensorDataColStateRepository.linAcceleReading.value }
                .collect { accel ->
                    // Compute approximate speed from acceleration
                    val computedSpeedMph = computeSpeedFromAcceleration(accel)

                    savedStateHandle[KEY_UI_STATE] = uiState.value.copy(computedSpeedMph = computedSpeedMph)
                }
        }

        // Observe GPS speed from repository (fallback data source)
        viewModelScope.launch {
            sensorDataColStateRepository.currentSpeedMps.collect { speedMs ->
                val now = System.currentTimeMillis()
                lastGpsUpdateTime = now

                savedStateHandle[KEY_UI_STATE] = uiState.value.copy(
                    speedMs = speedMs,
                    speedKmh = speedMs * 3.6,
                    speedMph = speedMs * 2.23694,
                    isUsingComputedSpeed = false,
                    lastUpdate = now
                )
            }
        }
    }

    /**
     * Compute approximate speed from linear acceleration
     * This is a simplified calculation and less accurate than GPS
     */
    private fun computeSpeedFromAcceleration(acceleration: Float): Double {
        // Simple integration over time (very rough estimate)
        // In real implementation, you'd need to track velocity over time
        val accelMagnitude = kotlin.math.abs(acceleration)

        // Convert acceleration to approximate speed (rough heuristic)
        // Vehicle acceleration typically 0-2 m/s² for normal driving
        return when {
            accelMagnitude < 0.5 -> 0.0 // Stationary
            accelMagnitude < 1.5 -> 15.0 // ~15 mph - slow driving
            accelMagnitude < 2.5 -> 30.0 // ~30 mph - moderate
            else -> 45.0 // ~45 mph - aggressive driving
        }
    }

    /**
     * Update classification based on variance
     */
    private fun updateClassification(variance: Double) {
        val classification = when {
            variance < 0.15 -> "Stationary"
            variance >= 0.15 && variance <= 1.5 -> "VEHICLE MOTION"
            variance > 2.5 -> "Walking/Running"
            else -> "Unknown"
        }
        savedStateHandle[KEY_UI_STATE] = uiState.value.copy(classification = classification)
    }


    private fun startDurationTimer() {
        viewModelScope.launch {
            while (tripStartTime != 0L) {
                val duration = System.currentTimeMillis() - tripStartTime
                val hours = duration / 3600000
                val minutes = (duration % 3600000) / 60000
                val seconds = (duration % 60000) / 1000
                val durationStr = "%02d:%02d:%02d".format(hours, minutes, seconds)
                savedStateHandle[KEY_UI_STATE] = uiState.value.copy(tripDuration = durationStr)
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    /**
     * Update GPS speed data
     * This should be called from HardwareModule when GPS data is received
     */
    fun updateGpsSpeed(speedMs: Double, accuracy: Float) {
        savedStateHandle[KEY_UI_STATE] = uiState.value.copy(
            speedMs = speedMs,
            speedKmh = speedMs * 3.6,
            speedMph = speedMs * 2.23694,
            accuracy = accuracy
        )
    }

    /**
     * Update variance and classification
     */
    fun updateVariance(variance: Double) {
        savedStateHandle[KEY_UI_STATE] = uiState.value.copy(variance = variance)

        val classification = when {
            variance < 0.15 -> "Stationary"
            variance >= 0.15 && variance <= 1.5 -> "VEHICLE MOTION"
            variance > 2.5 -> "Walking/Running"
            else -> "Unknown"
        }
        savedStateHandle[KEY_UI_STATE] = uiState.value.copy(classification = classification)
    }

    /**
     * Update timer progress (0.0 to 1.0)
     */
    fun updateTimerProgress(progress: Float) {
        savedStateHandle[KEY_UI_STATE] = uiState.value.copy(timerProgress = progress)
    }

    /**
     * Update trip ID
     */
    fun updateTripId(tripId: String) {
        savedStateHandle[KEY_UI_STATE] = uiState.value.copy(tripId = tripId)
    }
}

