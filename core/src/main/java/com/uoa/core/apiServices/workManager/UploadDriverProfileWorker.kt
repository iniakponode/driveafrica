//package com.uoa.core.apiServices.workManager
//
//import android.content.Context
//import android.net.ConnectivityManager
//import android.net.NetworkCapabilities
//import androidx.work.CoroutineWorker
//import androidx.work.WorkerParameters
//import com.uoa.core.apiServices.models.driverProfile.DriverProfileCreate
//import com.uoa.core.apiServices.models.rawSensorModels.RawSensorDataCreate
//import com.uoa.core.apiServices.services.driverProfileApiService.DriverProfileApiRepository
//import com.uoa.core.database.repository.DriverProfileRepository
//import com.uoa.core.notifications.VehicleNotificationManager
//import com.uoa.core.utils.Resource
//
//class UploadDriverProfileWorker (
//    appContext: Context,
//    workerParams: WorkerParameters,
//    private val repository: DriverProfileApiRepository,
//    private val localDriverRepository: DriverProfileRepository
//) : CoroutineWorker(appContext, workerParams) {
//
//    override suspend fun doWork(): Result {
//        val vehicleNotificationManager = VehicleNotificationManager(applicationContext)
//        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        val network = connectivityManager.activeNetwork
//        val capabilities = connectivityManager.getNetworkCapabilities(network)
//        val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
//
//        if (isConnected) {
//            val localDriverProfile = localDriverRepository.getDriverProfileBySyncStatus(false)
//            val driverDataList = localDriverProfile.map {
//                DriverProfileCreate(
//                    driverProfileId = it.driverProfileId,
//                    email = it.email,
//                    sync = true
//                )
//            }
//
//            vehicleNotificationManager.displayNotification("Data Upload", "Uploading Driver Profile...")
//            val result = repository.batchCreateDriverProfiles(driverDataList)
//            return if (result is Resource.Success) {
//                vehicleNotificationManager.displayNotification("Data Upload", "Driver Profile uploaded successfully.")
////                Delete Uploaded Data
////                val idsToDelete = localDriverProfile.map { it.driverProfileId }
////                localDriverRepository.ba(idsToDelete)
//
//                Result.success()
//            } else {
//                vehicleNotificationManager.displayNotification("Data Upload", "Failed to upload sensor data. Retrying...")
//                Result.retry()
//            }
//        } else {
//            return Result.retry()
//        }
//    }
//}
//
