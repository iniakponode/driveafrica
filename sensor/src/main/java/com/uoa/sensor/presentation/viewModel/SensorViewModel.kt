package com.uoa.sensor.presentation.viewModel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.sensor.repository.SensorDataColStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SensorViewModel @Inject constructor(
    private val sensorDataColStateRepository: SensorDataColStateRepository,
    @ApplicationContext val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Flows exposed to the UI:
    // 1. Collection status: whether sensor data is being collected
    val collectionStatus: StateFlow<Boolean> = sensorDataColStateRepository.collectionStatus

    // 2. Vehicle movement status: whether the vehicle is currently moving
    val isVehicleMoving: StateFlow<Boolean> = sensorDataColStateRepository.isVehicleMoving

    // 3. Trip start status: whether a trip has started
    val tripStartStatus: StateFlow<Boolean> = sensorDataColStateRepository.tripStartStatus

    // 4. Linear Acceleration Reading: Keep track of the Linear acceleration reading
    // that checks vehicle movement
    val linAcceleReading=sensorDataColStateRepository.linAcceleReading

    val readableAcceleration=sensorDataColStateRepository.readableAcceleration

    val movementType=sensorDataColStateRepository.movementLabel

    // Distance travelled in meters across the current path
    private val _distanceTravelled = MutableStateFlow(
        savedStateHandle.get<Double>("distanceTravelled") ?: 0.0
    )
    val distanceTravelled: StateFlow<Double> = _distanceTravelled

    // List of recorded path points
    private val _pathPoints = MutableStateFlow(
        savedStateHandle.get<ArrayList<GeoPoint>>("pathPoints")?.toList() ?: emptyList()
    )
    val pathPoints: StateFlow<List<GeoPoint>> = _pathPoints


    // Expose one‐off “start trip” and “stop trip” events
    private val _startTripEvent = MutableSharedFlow<UUID>(replay = 0)
    val startTripEvent: SharedFlow<UUID> = _startTripEvent

    private val _stopTripEvent = MutableSharedFlow<UUID>(replay = 0)
    val stopTripEvent: SharedFlow<UUID> = _stopTripEvent

    fun onAutoStartNeeded(driverProfileId: UUID, tripId: UUID) {
        // update internal state
        viewModelScope.launch {
            _startTripEvent.emit(tripId)
        }
    }

    fun onAutoStopNeeded(tripId: UUID) {
        viewModelScope.launch {
            _stopTripEvent.emit(tripId)
        }
    }

    fun addLocation(point: GeoPoint) {
        val currentPath = _pathPoints.value
        if (currentPath.isNotEmpty()) {
            val lastPoint = currentPath.last()
            _distanceTravelled.value = _distanceTravelled.value + lastPoint.distanceToAsDouble(point)
        }
        _pathPoints.value = currentPath + point
    }

    override fun onCleared() {
        savedStateHandle["distanceTravelled"] = _distanceTravelled.value
        savedStateHandle["pathPoints"] = ArrayList(_pathPoints.value)
        super.onCleared()
    }
}
