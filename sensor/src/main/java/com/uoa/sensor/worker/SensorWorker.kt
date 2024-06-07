package com.uoa.sensor.worker

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.uoa.sensor.hardware.HardwareModule
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

@HiltWorker
class SensorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val hardwareModule: HardwareModule
) : CoroutineWorker(context, workerParams) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        val taskType = inputData.getString("TASK_TYPE")
        val isLocationPermissionGranted = inputData.getBoolean("LOCATION_PERMISSION_GRANTED", false)
        return when (taskType) {
            "START" -> {
                hardwareModule.startDataCollection(isLocationPermissionGranted)
                try {
                    while (!isStopped) {
                        // Simulate continuous data collection
                        delay(1000)
                    }
                } catch (e: InterruptedException) {
                    Result.failure()
                }
                Result.success()
            }

            else -> Result.failure()
        }
    }

}
