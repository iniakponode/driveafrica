package com.uoa.sensor.repository

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

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
    val vehicleMovementStatus: StateFlow<Boolean> get() = _isVehicleMoving

    private val _linAcceleReading = mutableFloatStateOf(0f)
    val linAcceleReading: MutableState<Float> get()=_linAcceleReading

    private val _movementLabel = mutableStateOf("")
    val movementLabel: MutableState<String> get()=_movementLabel

    private val _movementStatus = MutableStateFlow(false)
    val movementStatus: StateFlow<Boolean> get()=_movementStatus


    /**
     * Update the data collection status
     */
    suspend fun updateCollectionStatus(status: Boolean) {
        _collectionStatus.emit(status)
    }

    /**
     * Update the movement type
     */
    suspend fun updateMovementType(movementType: String) {
        _movementLabel.value=movementType
    }

    /**
     * Update the movement status
     */
    suspend fun updateMovementStatus(ismoving: Boolean) {
        _movementStatus.value=ismoving
    }

    /**
     * Update Linear Acceleration reading
     */
    suspend fun updateLinearAcceleration(linAcceleReading: Double) {
        withContext(Dispatchers.IO) {
            _linAcceleReading.floatValue = linAcceleReading.toFloat()
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
    suspend fun updateVehicleMovementStatus(isVehicle: Boolean) {

            _isVehicleMoving.emit(isVehicle)

    }

    /**
     * Update the trip start (or end) status
     */
    suspend fun startTripStatus(tripStarted: Boolean) {
        _tripStartStatus.emit(tripStarted)
    }
}
