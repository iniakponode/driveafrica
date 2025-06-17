package com.uoa.core.apiServices.services.drivingTipApiService

// DrivingTipRepository.kt
import com.uoa.core.apiServices.models.drivingTipModels.DrivingTipCreate
import com.uoa.core.apiServices.models.drivingTipModels.DrivingTipResponse
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrivingTipApiRepository @Inject constructor(
    private val drivingTipApiService: DrivingTipApiService
) {

    // Create a new DrivingTip
    suspend fun createDrivingTip(drivingTip: DrivingTipCreate): Resource<DrivingTipResponse> = withContext(Dispatchers.IO) {
        try {
            val response = drivingTipApiService.createDrivingTip(drivingTip)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Retrieve a DrivingTip by ID
    suspend fun getDrivingTip(tipId: String): Resource<DrivingTipResponse> = withContext(Dispatchers.IO) {
        try {
            val response = drivingTipApiService.getDrivingTip(tipId)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("DrivingTip not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Retrieve all DrivingTips with optional pagination
    suspend fun getAllDrivingTips(skip: Int = 0, limit: Int = 50): Resource<List<DrivingTipResponse>> = withContext(Dispatchers.IO) {
        try {
            val responseList = drivingTipApiService.getAllDrivingTips(skip, limit)
            Resource.Success(responseList)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Update a DrivingTip
    suspend fun updateDrivingTip(tipId: String, drivingTip: DrivingTipCreate): Resource<DrivingTipResponse> = withContext(Dispatchers.IO) {
        try {
            val response = drivingTipApiService.updateDrivingTip(tipId, drivingTip)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Delete a DrivingTip
    suspend fun deleteDrivingTip(tipId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            drivingTipApiService.deleteDrivingTip(tipId)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("DrivingTip not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Batch create DrivingTips
    suspend fun batchCreateDrivingTips(drivingTips: List<DrivingTipCreate>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            drivingTipApiService.batchCreateDrivingTips(drivingTips)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Batch delete DrivingTips
    suspend fun batchDeleteDrivingTips(ids: List<String>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            drivingTipApiService.batchDeleteDrivingTips(ids)
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
