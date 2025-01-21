package com.uoa.core.apiServices.services.aiModellInputApiService

import com.uoa.core.apiServices.models.aiModelInputModels.AIModelInputCreate
import com.uoa.core.apiServices.models.aiModelInputModels.AIModelInputResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AIModelInputApiService {

    @POST("/api/ai_model_inputs/")
    suspend fun createAiModelInput(@Body aiModelInput: AIModelInputCreate): AIModelInputResponse

    @GET("/api/ai_model_inputs/{input_id}")
    suspend fun getAiModelInput(@Path("input_id") inputId: String): AIModelInputResponse

    @GET("/api/ai_model_inputs/")
    suspend fun getAllAiModelInputs(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100 // Adjusted limit value for large datasets
    ): List<AIModelInputResponse>

    @PUT("/api/ai_model_inputs/{input_id}")
    suspend fun updateAiModelInput(
        @Path("input_id") inputId: String,
        @Body aiModelInput: AIModelInputCreate
    ): AIModelInputResponse

    @DELETE("/api/ai_model_inputs/{input_id}")
    suspend fun deleteAiModelInput(@Path("input_id") inputId: String): Unit

    // Batch Operations
    @POST("/api/ai_model_inputs/batch_create")
    suspend fun batchCreateAiModelInputs(@Body aiModelInputs: List<AIModelInputCreate>): Map<String, String>

    @HTTP(method = "DELETE", path = "/api/ai_model_inputs/batch_delete", hasBody = true)
    suspend fun batchDeleteAiModelInputs(@Body ids: List<String>): Map<String, String>
}