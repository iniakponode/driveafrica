package com.uoa.sensor.repository

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorDataColStateRepository @Inject constructor() {

    // Mutable StateFlows to track collection status and vehicle movement
    private val _collectionStatus = MutableStateFlow(false)
    val collectionStatus: StateFlow<Boolean> get() = _collectionStatus

    private val _tripStartStatus= MutableStateFlow(false)
    val tripStartStatus: StateFlow<Boolean> get() = _tripStartStatus

    private val _isVehicleMoving = MutableStateFlow(false)
    val isVehicleMoving: StateFlow<Boolean> get() = _isVehicleMoving

    /**
     * Update the data collection status
     */
    suspend fun updateCollectionStatus(status: Boolean) {
        _collectionStatus.emit(status)
    }

    /**
     * Update the vehicle movement status
     */
    suspend fun updateVehicleMovementStatus(isMoving: Boolean) {
        _isVehicleMoving.emit(isMoving)
    }
    /**
     * Update the Trip End  Status
     */
    suspend fun startTripStatus(tripStatus:Boolean){
//        Log.d("SensorDataColStateRepository", "Emitting tripStartStatus: $tripStatus")
        _tripStartStatus.emit(tripStatus)
    }
}

