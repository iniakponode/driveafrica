package com.uoa.sensor.domain.usecases//package com.uoa.sensor.domain.usecases
//
//import android.content.Context
//import androidx.hilt.work.HiltWorker
//import androidx.work.CoroutineWorker
////import androidx.
//import androidx.work.WorkerParameters
//import com.uoa.network.data.repository.NetworkRepository
//import com.uoa.sensor.data.repository.SensorDataRepository
//import dagger.assisted.Assisted
//import dagger.assisted.AssistedInject
//
//@HiltWorker
//class SensorDataUploadWorker @AssistedInject constructor(
//    @Assisted private val appContext: Context,
//    @Assisted private val workerParams: WorkerParameters,
//    private val sensorDataRepository: SensorDataRepository,
//    private val networkRepository: NetworkRepository
//) : CoroutineWorker(appContext, workerParams) {
//
//    override suspend fun doWork(): Result {
//        return if (NetworkUtil.isNetworkAvailable(appContext)) {
//            try {
//                val unsyncedData = sensorDataRepository.getSensorDataBySyncStatus(synced = false)
//                if (unsyncedData.isNotEmpty()) {
//                    unsyncedData.forEach { data ->
//                        networkRepository.insertSensorData(data)
//                        sensorDataRepository.updateUploadStatus(id=data.id,true)
//                    }
//                }
//                Result.success()
//            } catch (e: Exception) {
//                Result.retry()
//            }
//        } else {
//            Result.retry()
//        }
//    }
//}
