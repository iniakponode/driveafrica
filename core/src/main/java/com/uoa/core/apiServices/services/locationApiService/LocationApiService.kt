// LocationApiService.kt
package com.uoa.core.apiServices.services.locationApiService

import com.uoa.core.apiServices.models.locationModels.LocationCreate
import com.uoa.core.apiServices.models.locationModels.LocationResponse
import retrofit2.http.*

interface LocationApiService {

    @POST("/api/locations/")
    suspend fun createLocation(@Body location: LocationCreate): LocationResponse

    @GET("/api/locations/{location_id}")
    suspend fun getLocation(@Path("location_id") locationId: String): LocationResponse

    @GET("/api/locations/")
    suspend fun getAllLocations(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100 // Adjusted limit value for large datasets
    ): List<LocationResponse>

    @PUT("/api/locations/{location_id}")
    suspend fun updateLocation(
        @Path("location_id") locationId: String,
        @Body location: LocationCreate
    ): LocationResponse

    @DELETE("/api/locations/{location_id}")
    suspend fun deleteLocation(@Path("location_id") locationId: String): Unit

    // Batch Operations
    @POST("/api/locations/batch_create")
    suspend fun batchCreateLocations(@Body locations: List<LocationCreate>): Map<String, String>

    @HTTP(method = "DELETE", path = "/api/locations/batch_delete", hasBody = true)
    suspend fun batchDeleteLocations(@Body ids: List<String>): Unit
}
