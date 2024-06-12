package com.uoa.sensor.presentation.viewModel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.uoa.sensor.data.repository.TripDataRepository
import com.uoa.sensor.hardware.HardwareModule
import com.uoa.sensor.worker.SensorWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
@HiltViewModel
class SensorViewModel @Inject constructor(
    private val workManager: WorkManager,
    private val hardwareModule: HardwareModule,
    private val tripRepository: TripDataRepository
) : ViewModel() {

    private val _collectionStatus = MutableStateFlow(false)
    val collectionStatus: StateFlow<Boolean> get() = _collectionStatus

    fun updateCollectionStatus(status: Boolean) {
        viewModelScope.launch {
            _collectionStatus.emit(status)
        }
    }

    private fun enqueueSensorWorker(
        taskType: String,
        isLocationPermissionGranted: Boolean,
        tripId: UUID
    ) {
        val data = Data.Builder()
            .putString("TASK_TYPE", taskType)
            .putString("TRIP_ID", tripId.toString())
            .putBoolean("LOCATION_PERMISSION_GRANTED", isLocationPermissionGranted)
            .build()

        val sensorWorkRequest = OneTimeWorkRequest.Builder(SensorWorker::class.java)
            .setInputData(data)
            .addTag("sensorWork")
            .build()

        workManager.enqueue(sensorWorkRequest).result.addListener({
            Log.d("SensorViewModel", "Work enqueued successfully")
            if (taskType == "START") {
                updateCollectionStatus(true)
            }
        }, { it.run() })
    }

    fun checkWorkManagerStatus() {
        viewModelScope.launch {
            val workQuery = WorkQuery.Builder
                .fromTags(listOf("sensorWork"))
                .addStates(listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING))
                .build()

            val workInfos = workManager.getWorkInfos(workQuery).get()
            val isWorkRunning = workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
            Log.d("SensorViewModel", "Work status: $isWorkRunning")

            if (_collectionStatus.value != isWorkRunning) {
                updateCollectionStatus(isWorkRunning)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun cancelSensorWorker() {
        viewModelScope.launch {
            workManager.cancelAllWorkByTag("sensorWork").result.addListener({
                Log.d("SensorViewModel", "Work cancelled successfully")
                hardwareModule.stopDataCollection()
                updateCollectionStatus(false)
            }, { it.run() })
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startSensorCollection(taskType: String, isLocationPermissionGranted: Boolean, tripId: UUID) {
//        hardwareModule.startDataCollection(isLocationPermissionGranted)
        enqueueSensorWorker(taskType, isLocationPermissionGranted, tripId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun stopSensorCollection() {
        cancelSensorWorker()

    }

}




