// UnsafeBehaviourApiService.kt
package com.uoa.core.apiServices.services.unsafeBehaviourApiService

import com.uoa.core.apiServices.models.unsafeBehaviourModels.UnsafeBehaviourCreate
import com.uoa.core.apiServices.models.unsafeBehaviourModels.UnsafeBehaviourResponse
import retrofit2.http.*

interface UnsafeBehaviourApiService {

    @POST("/api/unsafe_behaviours/")
    suspend fun createUnsafeBehaviour(@Body unsafeBehaviour: UnsafeBehaviourCreate): UnsafeBehaviourResponse

    @GET("/api/unsafe_behaviours/{behaviour_id}")
    suspend fun getUnsafeBehaviour(@Path("behaviour_id") behaviourId: String): UnsafeBehaviourResponse

    @GET("/api/unsafe_behaviours/")
    suspend fun getAllUnsafeBehaviours(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100 // Adjusted limit value for large datasets
    ): List<UnsafeBehaviourResponse>

    @PUT("/api/unsafe_behaviours/{behaviour_id}")
    suspend fun updateUnsafeBehaviour(
        @Path("behaviour_id") behaviourId: String,
        @Body unsafeBehaviour: UnsafeBehaviourCreate
    ): UnsafeBehaviourResponse

    @DELETE("/api/unsafe_behaviours/{behaviour_id}")
    suspend fun deleteUnsafeBehaviour(@Path("behaviour_id") behaviourId: String): Unit

    // Batch Operations
    @POST("/api/unsafe_behaviours/batch_create")
    suspend fun batchCreateUnsafeBehaviours(@Body unsafeBehaviours: List<UnsafeBehaviourCreate>): Map<String, String>

    @HTTP(method = "DELETE", path = "/api/unsafe_behaviours/batch_delete", hasBody = true)
    suspend fun batchDeleteUnsafeBehaviours(@Body ids: List<String>): Unit
}
