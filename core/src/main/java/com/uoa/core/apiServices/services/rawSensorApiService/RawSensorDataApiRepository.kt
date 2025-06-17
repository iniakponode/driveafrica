// RawSensorDataRepository.kt
package com.uoa.core.apiServices.services.rawSensorApiService

import com.uoa.core.apiServices.models.rawSensorModels.RawSensorDataCreate
import com.uoa.core.apiServices.models.rawSensorModels.RawSensorDataResponse
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RawSensorDataApiRepository @Inject constructor(
    private val rawSensorDataApiService: RawSensorDataApiService
) {

    // Create raw sensor data
    suspend fun createRawSensorData(data: RawSensorDataCreate): Resource<RawSensorDataResponse> = withContext(Dispatchers.IO) {
        try {
            val response = rawSensorDataApiService.createRawSensorData(data)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Retrieve raw sensor data by ID
    suspend fun getRawSensorData(dataId: String): Resource<RawSensorDataResponse> = withContext(Dispatchers.IO) {
        try {
            val response = rawSensorDataApiService.getRawSensorData(dataId)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("Sensor data not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Retrieve all raw sensor data with pagination
    suspend fun getAllRawSensorData(skip: Int = 0, limit: Int = 500): Resource<List<RawSensorDataResponse>> = withContext(Dispatchers.IO) {
        try {
            val dataList = rawSensorDataApiService.getAllRawSensorData(skip, limit)
            Resource.Success(dataList)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Update raw sensor data
    suspend fun updateRawSensorData(dataId: String, data: RawSensorDataCreate): Resource<RawSensorDataResponse> = withContext(Dispatchers.IO) {
        try {
            val response = rawSensorDataApiService.updateRawSensorData(dataId, data)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Delete raw sensor data
    suspend fun deleteRawSensorData(dataId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            rawSensorDataApiService.deleteRawSensorData(dataId)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("Sensor data not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Batch create raw sensor data
    suspend fun batchCreateRawSensorData(dataList: List<RawSensorDataCreate>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            rawSensorDataApiService.batchCreateRawSensorData(dataList)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Batch delete raw sensor data
    suspend fun batchDeleteRawSensorData(ids: List<String>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            rawSensorDataApiService.batchDeleteRawSensorData(ids)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }
}