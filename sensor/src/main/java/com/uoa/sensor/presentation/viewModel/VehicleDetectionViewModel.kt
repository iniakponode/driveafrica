package com.uoa.sensor.presentation.viewModel

import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.sensor.repository.SensorDataColStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Stable
@Parcelize
data class VehicleDetectionUiState(
    // Current state
    val currentState: String = "IDLE",

    // Fused Speed data
    val speedMs: Double = 0.0,
    val speedKmh: Double = 0.0,
    val speedMph: Double = 0.0,
    val accuracy: Float = 0f,

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

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@HiltViewModel
class VehicleDetectionViewModel @Inject constructor(
    private val sensorDataColStateRepository: SensorDataColStateRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "VehicleDetectionVM"
        private const val KEY_UI_STATE = "vehicle_detection_ui_state"
    }

    // Make the UI state survive process death and configuration changes.
    val uiState: StateFlow<VehicleDetectionUiState> = savedStateHandle.getStateFlow(KEY_UI_STATE, VehicleDetectionUiState())

    init {
        Log.d(TAG, "ViewModel initialized")
        observeSensorState()
        observeDrivingStateState()
        observeTripDuration()
    }

    private fun observeTripDuration() {
        viewModelScope.launch {
            sensorDataColStateRepository.tripDuration.collect { duration ->
                savedStateHandle[KEY_UI_STATE] = uiState.value.copy(tripDuration = duration)
            }
        }
    }

    private fun observeDrivingStateState() {
        viewModelScope.launch {
            combine(
                sensorDataColStateRepository.drivingState,
                sensorDataColStateRepository.drivingVariance,
                sensorDataColStateRepository.drivingSpeedMps,
                sensorDataColStateRepository.drivingAccuracy
            ) { state, variance, speedMps, accuracy ->
                Quad(state, variance, speedMps, accuracy)
            }.collect { (state, variance, speedMps, accuracy) ->
                val now = System.currentTimeMillis()
                savedStateHandle[KEY_UI_STATE] = uiState.value.copy(
                    currentState = state.name,
                    variance = variance,
                    speedMs = speedMps,
                    speedKmh = speedMps * 3.6,
                    speedMph = speedMps * 2.23694,
                    accuracy = accuracy,
                    lastUpdate = now
                )

                // Update classification based on variance
                updateClassification(variance)

                Log.v(TAG, "State update: ${state.name}, Speed: ${speedMps * 2.23694} mph, Variance: $variance")
            }
        }
    }

    private fun observeSensorState() {
        // Observe collection status
        viewModelScope.launch {
            sensorDataColStateRepository.collectionStatus.collect { isCollecting ->
                savedStateHandle[KEY_UI_STATE] = uiState.value.copy(isRecording = isCollecting)

                if (isCollecting) {
                    startDurationTimer()
                }
            }
        }

        viewModelScope.launch {
            sensorDataColStateRepository.currentTripId.collect { tripId ->
                savedStateHandle[KEY_UI_STATE] = uiState.value.copy(tripId = tripId?.toString() ?: "")
                Log.d(TAG, "Trip ID updated: $tripId")
            }
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
            // Trip duration is updated by SensorDataColStateRepository.
            kotlinx.coroutines.delay(0)
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
    fun updateTripId(tripId: UUID) {
        savedStateHandle[KEY_UI_STATE] = uiState.value.copy(tripId = tripId.toString())
    }
}
