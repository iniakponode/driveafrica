package com.uoa.sensor.presentation.viewModel

import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.await
import com.uoa.core.mlclassifier.MinMaxValuesLoader
import com.uoa.sensor.hardware.HardwareModule
import com.uoa.sensor.repository.SensorDataColStateRepository
//import com.uoa.sensor.worker.SensorWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
@HiltViewModel
class SensorViewModel @Inject constructor(
//    private val workManager: WorkManager,
//    private val hardwareModule: HardwareModule,
    private val sensorDataColStateRepository: SensorDataColStateRepository
) : ViewModel() {

    val collectionStatus: StateFlow<Boolean> = sensorDataColStateRepository.collectionStatus
    val isVehicleMoving: StateFlow<Boolean> = sensorDataColStateRepository.isVehicleMoving
    val tripEndStatus: StateFlow<Boolean> = sensorDataColStateRepository.tripStartStatus


    fun updateCollectionStatus(status: Boolean) {
        viewModelScope.launch {
            sensorDataColStateRepository.updateCollectionStatus(status)
        }
    }
    fun updateVehicleMovementStatus(status: Boolean){
        viewModelScope.launch{
            sensorDataColStateRepository.updateVehicleMovementStatus(status)
        }
    }
//    private val _collectionStatus = MutableStateFlow(false)
//    val collectionStatus: StateFlow<Boolean> get() = _collectionStatus

//    private val _isVehicleMoving=MutableStateFlow(false)
//    val isVehicleMoving: StateFlow<Boolean> get()= _isVehicleMoving

//    fun updateCollectionStatus(status: Boolean) {
//        sensorDataColStateRepository.collectionStatus.value = status
//    }
//
//    fun updateVehicleMovementStatus(status: Boolean) {
//        sensorDataColStateRepository.isVehicleMoving.value = status
//    }

//    fun updateCollectionStatus(status: Boolean) {
//        viewModelScope.launch {
//            _collectionStatus.emit(status)
//        }
//    }
//    fun updateVehicleMovementStatus(status: Boolean){
//        viewModelScope.launch{
//            _isVehicleMoving.emit(status)
//        }
//    }

//    private fun enqueueSensorWorker(
//        taskType: String,
//        isLocationPermissionGranted: Boolean,
//        tripId: UUID
//    ) {
//        val data = Data.Builder()
//            .putString("TASK_TYPE", taskType)
//            .putString("TRIP_ID", tripId.toString())
//            .putBoolean("LOCATION_PERMISSION_GRANTED", isLocationPermissionGranted)
//            .build()
//
//        val sensorWorkRequest = OneTimeWorkRequest.Builder(SensorWorker::class.java)
//            .setInputData(data)
//            .addTag("sensorWork")
//            .build()
//
//        workManager.enqueue(sensorWorkRequest).result.addListener({
//            Log.d("SensorViewModel", "Work enqueued successfully")
//            if (taskType == "START") {
//                updateCollectionStatus(true)
//                updateVehicleMovementStatus(hardwareModule.isVehicleMoving())
//            }
//        }, { it.run() })
//    }

//    fun checkWorkManagerStatus() {
//        viewModelScope.launch {
//            val workQuery = WorkQuery.Builder
//                .fromTags(listOf("sensorWork"))
//                .addStates(listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING))
//                .build()
//
//            val workInfos = workManager.getWorkInfos(workQuery).get()
//            val isWorkRunning = workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
//            Log.d("SensorViewModel", "Work status: $isWorkRunning")
//
//            if (_collectionStatus.value != isWorkRunning) {
//                updateCollectionStatus(isWorkRunning)
//                updateVehicleMovementStatus(hardwareModule.isVehicleMoving())
//            }
//        }
//    }
//
//    fun cancelSensorWorker() {
//        viewModelScope.launch {
//            try {
//                // Await the cancellation of all work with the tag "sensorWork"
//                workManager.cancelAllWorkByTag("sensorWork").await()
//
//                Log.d("SensorViewModel", "Work cancelled successfully")
//
//                // Call the suspend function
//
//                hardwareModule.stopDataCollection()
//                updateCollectionStatus(false)
//                updateVehicleMovementStatus(hardwareModule.isVehicleMoving())
//            } catch (e: Exception) {
//                Log.e("SensorViewModel", "Error cancelling work", e)
//            }
//        }
//    }

//    fun startSensorCollection(taskType: String, isLocationPermissionGranted: Boolean, tripId: UUID) {
////        hardwareModule.startDataCollection(isLocationPermissionGranted)
//        enqueueSensorWorker(taskType, isLocationPermissionGranted, tripId)
//    }
//
//    fun stopSensorCollection() {
//        cancelSensorWorker()
//
//    }

}




