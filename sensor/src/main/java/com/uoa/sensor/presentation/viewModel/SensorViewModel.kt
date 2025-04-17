package com.uoa.sensor.presentation.viewModel

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.lifecycle.ViewModel
import com.uoa.sensor.repository.SensorDataColStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SensorViewModel @Inject constructor(
    private val sensorDataColStateRepository: SensorDataColStateRepository,
    @ApplicationContext val context: Context
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
}
