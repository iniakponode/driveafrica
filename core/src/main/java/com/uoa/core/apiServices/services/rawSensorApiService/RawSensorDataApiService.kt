package com.uoa.core.apiServices.services.rawSensorApiService

import com.uoa.core.apiServices.models.rawSensorModels.RawSensorDataCreate
import com.uoa.core.apiServices.models.rawSensorModels.RawSensorDataResponse
import retrofit2.Call
import retrofit2.http.*

interface RawSensorDataApiService {

    @POST("/api/raw_sensor_data/")
    suspend fun createRawSensorData(@Body data: RawSensorDataCreate): RawSensorDataResponse

    @GET("/api/raw_sensor_data/{data_id}")
    suspend fun getRawSensorData(@Path("data_id") dataId: String): RawSensorDataResponse

    @GET("/api/raw_sensor_data/")
    suspend fun getAllRawSensorData(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 500 // Adjusted limit value for large datasets
    ): List<RawSensorDataResponse>

    @PUT("/api/raw_sensor_data/{data_id}")
    suspend fun updateRawSensorData(
        @Path("data_id") dataId: String,
        @Body data: RawSensorDataCreate
    ): RawSensorDataResponse

    @DELETE("/api/raw_sensor_data/{data_id}")
    suspend fun deleteRawSensorData(@Path("data_id") dataId: String): Unit

    // Batch Operations
    @POST("/api/raw_sensor_data/batch_create")
    suspend fun batchCreateRawSensorData(@Body dataList: List<RawSensorDataCreate>): Map<String, String>

    @HTTP(method = "DELETE", path = "/api/raw_sensor_data/batch_delete", hasBody = true)
    suspend fun batchDeleteRawSensorData(@Body ids: List<String>): Map<String, String>
}