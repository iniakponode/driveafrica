package com.uoa.core.apiServices.services.aiModellInputApiService

import com.uoa.core.apiServices.models.aiModelInputModels.AIModelInputCreate
import com.uoa.core.apiServices.models.aiModelInputModels.AIModelInputResponse
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIModelInputApiRepository @Inject constructor(
    private val aiModelInputApiService: AIModelInputApiService
) {

    // Create a new AiModelInput
    suspend fun createAiModelInput(aiModelInput: AIModelInputCreate): Resource<AIModelInputResponse> = withContext(Dispatchers.IO) {
        try {
            val response = aiModelInputApiService.createAiModelInput(aiModelInput)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Retrieve an AiModelInput by ID
    suspend fun getAiModelInput(inputId: String): Resource<AIModelInputResponse> = withContext(Dispatchers.IO) {
        try {
            val response = aiModelInputApiService.getAiModelInput(inputId)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("AiModelInput not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Retrieve all AiModelInputs with optional pagination
    suspend fun getAllAiModelInputs(skip: Int = 0, limit: Int = 100): Resource<List<AIModelInputResponse>> = withContext(Dispatchers.IO) {
        try {
            val responseList = aiModelInputApiService.getAllAiModelInputs(skip, limit)
            Resource.Success(responseList)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Update an AiModelInput
    suspend fun updateAiModelInput(inputId: String, aiModelInput: AIModelInputCreate): Resource<AIModelInputResponse> = withContext(Dispatchers.IO) {
        try {
            val response = aiModelInputApiService.updateAiModelInput(inputId, aiModelInput)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Delete an AiModelInput
    suspend fun deleteAiModelInput(inputId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            aiModelInputApiService.deleteAiModelInput(inputId)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("AiModelInput not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Batch create AiModelInputs
    suspend fun batchCreateAiModelInputs(aiModelInputs: List<AIModelInputCreate>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            aiModelInputApiService.batchCreateAiModelInputs(aiModelInputs)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Batch delete AiModelInputs
    suspend fun batchDeleteAiModelInputs(ids: List<String>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            aiModelInputApiService.batchDeleteAiModelInputs(ids)
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