// UnsafeBehaviourRepository.kt
package com.uoa.core.apiServices.services.unsafeBehaviourApiService

import com.uoa.core.apiServices.models.unsafeBehaviourModels.UnsafeBehaviourCreate
import com.uoa.core.apiServices.models.unsafeBehaviourModels.UnsafeBehaviourResponse
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnsafeBehaviourApiRepository @Inject constructor(
    private val unsafeBehaviourApiService: UnsafeBehaviourApiService
) {

    // Create a new UnsafeBehaviour
    suspend fun createUnsafeBehaviour(unsafeBehaviour: UnsafeBehaviourCreate): Resource<UnsafeBehaviourResponse> = withContext(Dispatchers.IO) {
        try {
            val response = unsafeBehaviourApiService.createUnsafeBehaviour(unsafeBehaviour)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Retrieve an UnsafeBehaviour by ID
    suspend fun getUnsafeBehaviour(behaviourId: String): Resource<UnsafeBehaviourResponse> = withContext(Dispatchers.IO) {
        try {
            val response = unsafeBehaviourApiService.getUnsafeBehaviour(behaviourId)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("UnsafeBehaviour not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Retrieve all UnsafeBehaviours with optional pagination
    suspend fun getAllUnsafeBehaviours(skip: Int = 0, limit: Int = 100): Resource<List<UnsafeBehaviourResponse>> = withContext(Dispatchers.IO) {
        try {
            val responseList = unsafeBehaviourApiService.getAllUnsafeBehaviours(skip, limit)
            Resource.Success(responseList)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Update an UnsafeBehaviour
    suspend fun updateUnsafeBehaviour(behaviourId: String, unsafeBehaviour: UnsafeBehaviourCreate): Resource<UnsafeBehaviourResponse> = withContext(Dispatchers.IO) {
        try {
            val response = unsafeBehaviourApiService.updateUnsafeBehaviour(behaviourId, unsafeBehaviour)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Delete an UnsafeBehaviour
    suspend fun deleteUnsafeBehaviour(behaviourId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            unsafeBehaviourApiService.deleteUnsafeBehaviour(behaviourId)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("UnsafeBehaviour not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Batch create UnsafeBehaviours
    suspend fun batchCreateUnsafeBehaviours(unsafeBehaviours: List<UnsafeBehaviourCreate>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            unsafeBehaviourApiService.batchCreateUnsafeBehaviours(unsafeBehaviours)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Batch delete UnsafeBehaviours
    suspend fun batchDeleteUnsafeBehaviours(ids: List<String>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            unsafeBehaviourApiService.batchDeleteUnsafeBehaviours(ids)
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
