// DrivingTipApiService.kt
package com.uoa.core.apiServices.services.drivingTipApiService

import com.uoa.core.apiServices.models.drivingTipModels.DrivingTipCreate
import com.uoa.core.apiServices.models.drivingTipModels.DrivingTipResponse
import retrofit2.http.*

interface DrivingTipApiService {

    @POST("/api/driving_tips/")
    suspend fun createDrivingTip(@Body drivingTip: DrivingTipCreate): DrivingTipResponse

    @GET("/api/driving_tips/{tip_id}")
    suspend fun getDrivingTip(@Path("tip_id") tipId: String): DrivingTipResponse

    @GET("/api/driving_tips/")
    suspend fun getAllDrivingTips(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50 // Adjusted limit value
    ): List<DrivingTipResponse>

    @PUT("/api/driving_tips/{tip_id}")
    suspend fun updateDrivingTip(
        @Path("tip_id") tipId: String,
        @Body drivingTip: DrivingTipCreate
    ): DrivingTipResponse

    @DELETE("/api/driving_tips/{tip_id}")
    suspend fun deleteDrivingTip(@Path("tip_id") tipId: String): Unit

    // Batch Operations
    @POST("/api/driving_tips/batch_create")
    suspend fun batchCreateDrivingTips(@Body drivingTips: List<DrivingTipCreate>): Map<String, String>

    @HTTP(method = "DELETE", path = "/api/driving_tips/batch_delete", hasBody = true)
    suspend fun batchDeleteDrivingTips(@Body ids: List<String>): Unit
}
