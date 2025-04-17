package com.uoa.sensor.repository

import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class SensorDataColStateRepository @Inject constructor() {

    // Tracks whether data collection is active (e.g., sensors running)
    private val _collectionStatus = MutableStateFlow(false)
    val collectionStatus: StateFlow<Boolean> get() = _collectionStatus

    // Tracks whether a trip has started
    private val _tripStartStatus = MutableStateFlow(false)
    val tripStartStatus: StateFlow<Boolean> get() = _tripStartStatus

    // Tracks whether the vehicle is currently moving
    private val _isVehicleMoving = MutableStateFlow(false)
    val isVehicleMoving: StateFlow<Boolean> get() = _isVehicleMoving

    private val _linAcceleReading = mutableFloatStateOf(0f)
    val linAcceleReading: MutableState<Float> get()=_linAcceleReading

    /**
     * Update the data collection status
     */
    suspend fun updateCollectionStatus(status: Boolean) {
        _collectionStatus.emit(status)
    }

    /**
     * Update Linear Acceleration reading
     */
    suspend fun updateLinearAcceleration(linAcceleReading: Float) {
        withContext(Dispatchers.IO) {
            _linAcceleReading.floatValue = linAcceleReading
        }
    }

    // Human-readable derived property
    val readableAcceleration: androidx.compose.runtime.State<String> =
        androidx.compose.runtime.derivedStateOf {
            _linAcceleReading.floatValue.toReadableAcceleration()
        }

    // Extension function to convert to readable value
    private fun Float.toReadableAcceleration(): String {
        val accelerationKmh = this * 3.6f
        val threshold = 0.5f
        return when {
            accelerationKmh > threshold -> "(+${"%.1f".format(accelerationKmh)} km/h per second)"
            accelerationKmh < -threshold -> "(${ "%.1f".format(accelerationKmh) } km/h per second)"
            else -> "Converting(km/h per second)..."
        }
        }

    /**
     * Update the vehicle movement status
     */
    suspend fun updateVehicleMovementStatus(isMoving: Boolean) {
        _isVehicleMoving.emit(isMoving)
    }

    /**
     * Update the trip start (or end) status
     */
    suspend fun startTripStatus(tripStarted: Boolean) {
        _tripStartStatus.emit(tripStarted)
    }
}
