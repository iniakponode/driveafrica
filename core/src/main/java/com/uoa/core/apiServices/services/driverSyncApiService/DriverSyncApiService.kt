package com.uoa.core.apiServices.services.driverSyncApiService

import com.uoa.core.apiServices.models.driverSyncModels.DriverSyncPayload
import com.uoa.core.apiServices.models.driverSyncModels.DriverSyncResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface DriverSyncApiService {

    @POST("/api/driver/sync")
    suspend fun syncDriverData(@Body payload: DriverSyncPayload): DriverSyncResponse
}
