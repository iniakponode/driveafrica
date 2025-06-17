// EmbeddingRepository.kt
package com.uoa.core.apiServices.services.embeddingApiService

import com.uoa.core.apiServices.models.embeddingModels.EmbeddingCreate
import com.uoa.core.apiServices.models.embeddingModels.EmbeddingResponse
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbeddingApiRepository @Inject constructor(
    private val embeddingApiService: EmbeddingApiService
) {

    // Create a new Embedding
    suspend fun createEmbedding(embedding: EmbeddingCreate): Resource<EmbeddingResponse> = withContext(Dispatchers.IO) {
        try {
            val response = embeddingApiService.createEmbedding(embedding)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Retrieve an Embedding by ID
    suspend fun getEmbedding(embeddingId: String): Resource<EmbeddingResponse> = withContext(Dispatchers.IO) {
        try {
            val response = embeddingApiService.getEmbedding(embeddingId)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("Embedding not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Retrieve all Embeddings with optional pagination
    suspend fun getAllEmbeddings(skip: Int = 0, limit: Int = 100): Resource<List<EmbeddingResponse>> = withContext(Dispatchers.IO) {
        try {
            val responseList = embeddingApiService.getAllEmbeddings(skip, limit)
            Resource.Success(responseList)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Update an Embedding
    suspend fun updateEmbedding(embeddingId: String, embedding: EmbeddingCreate): Resource<EmbeddingResponse> = withContext(Dispatchers.IO) {
        try {
            val response = embeddingApiService.updateEmbedding(embeddingId, embedding)
            Resource.Success(response)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Delete an Embedding
    suspend fun deleteEmbedding(embeddingId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            embeddingApiService.deleteEmbedding(embeddingId)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Resource.Error("Embedding not found.")
                else -> Resource.Error("Server error (${e.code()}): ${e.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Batch create Embeddings
    suspend fun batchCreateEmbeddings(embeddings: List<EmbeddingCreate>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            embeddingApiService.batchCreateEmbeddings(embeddings)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: HttpException) {
            Resource.Error("Server error (${e.code()}): ${e.message()}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    // Batch delete Embeddings
    suspend fun batchDeleteEmbeddings(ids: List<String>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            embeddingApiService.batchDeleteEmbeddings(ids)
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
