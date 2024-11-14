//// RawSensorDataRepository.kt
//package com.uoa.core.apiServices.services.rawSensorApiService
//
//import com.uoa.core.apiServices.models.rawSensorModels.RawSensorDataCreate
//import com.uoa.core.apiServices.models.rawSensorModels.RawSensorDataResponse
//import com.uoa.core.utils.Resource
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class RawSensorDataRepository @Inject constructor(
//    private val rawSensorDataApiService: RawSensorDataApiService
//) {
//
//    // Create raw sensor data
//    suspend fun createRawSensorData(data: RawSensorDataCreate): Resource<RawSensorDataResponse> = withContext(Dispatchers.IO) {
//        try {
//            val response = rawSensorDataApiService.createRawSensorData(data)
//            Resource.Success(response)
//        } catch (e: Exception) {
//            Resource.Error("Error creating raw sensor data: ${e.localizedMessage}")
//        }
//    }
//
//    // Get raw sensor data by ID
//    suspend fun getRawSensorData(dataId: String): Resource<RawSensorDataResponse> = withContext(Dispatchers.IO) {
//        try {
//            val response = rawSensorDataApiService.getRawSensorData(dataId)
//            Resource.Success(response)
//        } catch (e: Exception) {
//            Resource.Error("Error fetching raw sensor data: ${e.localizedMessage}")
//        }
//    }
//
//    // Get all raw sensor data
//    suspend fun getAllRawSensorData(skip: Int = 0, limit: Int = 100): Resource<List<RawSensorDataResponse>> = withContext(Dispatchers.IO) {
//        try {
//            val responseList = rawSensorDataApiService.getAllRawSensorData(skip, limit)
//            Resource.Success(responseList)
//        } catch (e: Exception) {
//            Resource.Error("Error fetching raw sensor data list: ${e.localizedMessage}")
//        }
//    }
//
//    // Update raw sensor data
//    suspend fun updateRawSensorData(dataId: String, data: RawSensorDataCreate): Resource<RawSensorDataResponse> = withContext(Dispatchers.IO) {
//        try {
//            val response = rawSensorDataApiService.updateRawSensorData(dataId, data)
//            Resource.Success(response)
//        } catch (e: Exception) {
//            Resource.Error("Error updating raw sensor data: ${e.localizedMessage}")
//        }
//    }
//
//    // Delete raw sensor data
//    suspend fun deleteRawSensorData(dataId: String): Resource<Unit> = withContext(Dispatchers.IO) {
//        try {
//            rawSensorDataApiService.deleteRawSensorData(dataId)
//            Resource.Success(Unit)
//        } catch (e: Exception) {
//            Resource.Error("Error deleting raw sensor data: ${e.localizedMessage}")
//        }
//    }
//
//    // Batch create raw sensor data
//    suspend fun batchCreateRawSensorData(dataList: List<RawSensorDataCreate>): Resource<Unit> = withContext(Dispatchers.IO) {
//        try {
//            rawSensorDataApiService.batchCreateRawSensorData(dataList)
//            Resource.Success(Unit)
//        } catch (e: Exception) {
//            Resource.Error("Error batch creating raw sensor data: ${e.localizedMessage}")
//        }
//    }
//
//    // Batch delete raw sensor data
//    suspend fun batchDeleteRawSensorData(ids: List<String>): Resource<Unit> = withContext(Dispatchers.IO) {
//        try {
//            rawSensorDataApiService.batchDeleteRawSensorData(ids)
//            Resource.Success(Unit)
//        } catch (e: Exception) {
//            Resource.Error("Error batch deleting raw sensor data: ${e.localizedMessage}")
//        }
//    }
//}
