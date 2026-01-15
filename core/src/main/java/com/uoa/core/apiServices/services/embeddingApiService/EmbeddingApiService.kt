// EmbeddingApiService.kt
package com.uoa.core.apiServices.services.embeddingApiService

import com.uoa.core.apiServices.models.embeddingModels.EmbeddingCreate
import com.uoa.core.apiServices.models.embeddingModels.EmbeddingResponse
import retrofit2.http.*

interface EmbeddingApiService {

    @POST("/api/embeddings/")
    suspend fun createEmbedding(@Body embedding: EmbeddingCreate): EmbeddingResponse

    @GET("/api/embeddings/{embedding_id}")
    suspend fun getEmbedding(@Path("embedding_id") embeddingId: String): EmbeddingResponse

    @GET("/api/embeddings/")
    suspend fun getAllEmbeddings(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100 // Adjusted limit value for large datasets
    ): List<EmbeddingResponse>

    @PUT("/api/embeddings/{embedding_id}")
    suspend fun updateEmbedding(
        @Path("embedding_id") embeddingId: String,
        @Body embedding: EmbeddingCreate
    ): EmbeddingResponse

    @DELETE("/api/embeddings/{embedding_id}")
    suspend fun deleteEmbedding(@Path("embedding_id") embeddingId: String): Unit

    // Batch Operations
    @POST("/api/embeddings/batch_create")
    suspend fun batchCreateEmbeddings(@Body embeddings: List<EmbeddingCreate>): Map<String, String>

    @HTTP(method = "DELETE", path = "/api/embeddings/batch_delete", hasBody = true)
    suspend fun batchDeleteEmbeddings(@Body ids: List<String>): Unit
}
