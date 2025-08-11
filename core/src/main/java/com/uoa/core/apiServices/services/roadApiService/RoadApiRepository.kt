package com.uoa.core.apiServices.services.roadApiService

import com.uoa.core.apiServices.models.roadModels.RoadCreate
import com.uoa.core.apiServices.models.roadModels.RoadResponse
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoadApiRepository @Inject constructor(
    private val roadApiService: RoadApiService
) {

    suspend fun createRoad(road: RoadCreate): Resource<RoadResponse> = withContext(Dispatchers.IO) {
        try {
            val response = roadApiService.createRoad(road)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    suspend fun getRoad(roadId: String): Resource<RoadResponse> = withContext(Dispatchers.IO) {
        try {
            val response = roadApiService.getRoad(roadId)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("Road not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    suspend fun getAllRoads(skip: Int = 0, limit: Int = 100): Resource<List<RoadResponse>> = withContext(Dispatchers.IO) {
        try {
            val responseList = roadApiService.getAllRoads(skip, limit)
            Resource.Success(responseList)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    suspend fun updateRoad(roadId: String, road: RoadCreate): Resource<RoadResponse> = withContext(Dispatchers.IO) {
        try {
            val response = roadApiService.updateRoad(roadId, road)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    suspend fun deleteRoad(roadId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            roadApiService.deleteRoad(roadId)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("Road not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // ----------------- Batch Operations -----------------

    // RoadApiRepository.kt
    suspend fun batchCreateRoads(roadList: List<RoadCreate>): Resource<List<RoadResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val resp = roadApiService.batchCreateRoads(roadList)
                Resource.Success(resp)
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                Resource.Error("Server error (${e.code()}): ${body ?: e.message()}")
            } catch (e: IOException) {
                Resource.Error("Network error: Please check your internet connection.")
            } catch (e: Exception) {
                Resource.Error("Unexpected error: ${e.localizedMessage}")
            }
        }


    suspend fun batchDeleteRoads(ids: List<String>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            roadApiService.batchDeleteRoads(ids)
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
