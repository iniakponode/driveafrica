package com.uoa.sensor.presentation.viewModel

import android.content.Context
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.sensor.repository.SensorDataColStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.UUID
import com.uoa.core.model.Road
import javax.inject.Inject

@HiltViewModel
class SensorViewModel @Inject constructor(
    private val sensorDataColStateRepository: SensorDataColStateRepository,
    @ApplicationContext val context: Context
) : ViewModel() {

    // Flows exposed to the UI:
    val collectionStatus: StateFlow<Boolean> = sensorDataColStateRepository.collectionStatus
    val isVehicleMoving: StateFlow<Boolean> = sensorDataColStateRepository.isVehicleMoving
    val tripStartStatus: StateFlow<Boolean> = sensorDataColStateRepository.tripStartStatus
    val linAcceleReading: State<Float> = sensorDataColStateRepository.linAcceleReading
    val readableAcceleration: State<String> = sensorDataColStateRepository.readableAcceleration
    val movementType: State<String> = sensorDataColStateRepository.movementLabel
    val distanceTravelled: StateFlow<Double> = sensorDataColStateRepository.distanceTravelled
    val pathPoints: StateFlow<List<GeoPoint>> = sensorDataColStateRepository.pathPoints
    val nearbyRoads: StateFlow<List<Road>> = sensorDataColStateRepository.nearbyRoads
    val speedLimit: StateFlow<Int> = sensorDataColStateRepository.speedLimit
    val currentLocation: StateFlow<GeoPoint?> = sensorDataColStateRepository.currentLocation
    val fusedSpeedMps: StateFlow<Double> = sensorDataColStateRepository.fusedSpeedMps

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

    fun addLocation(point: GeoPoint, distance: Double, roads: List<Road>, speedLimit: Int) {
        sensorDataColStateRepository.updateLocation(point, distance, roads, speedLimit)
    }
}