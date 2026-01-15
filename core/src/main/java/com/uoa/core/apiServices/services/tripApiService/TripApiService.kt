package com.uoa.core.apiServices.services.tripApiService

import com.uoa.core.apiServices.models.tripModels.TripCreate
import com.uoa.core.apiServices.models.tripModels.TripResponse
import retrofit2.Call
import retrofit2.http.*
import java.util.UUID

interface TripApiService {

    @POST("/api/trips/")
    suspend fun createTrip(@Body trip: TripCreate): TripResponse

    @GET("/api/trips/{trip_id}")
    suspend fun getTrip(@Path("trip_id") tripId: String): TripResponse

    @GET("/api/trips/")
    suspend fun getAllTrips(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50 // Adjusted limit value
    ): List<TripResponse>

    @PUT("/api/trips/{trip_id}")
    suspend fun updateTrip(
        @Path("trip_id") tripId: UUID,
        @Body trip: TripCreate
    ): TripResponse

    @DELETE("/api/trips/{trip_id}")
    suspend fun deleteTrip(@Path("trip_id") tripId: String): Unit

    // Batch Operations
    @POST("/api/trips/batch_create")
    suspend fun batchCreateTrips(@Body trips: List<TripCreate>): List<TripResponse>

    @HTTP(method = "DELETE", path = "/api/trips/batch_delete", hasBody = true)
    suspend fun batchDeleteTrips(@Body ids: List<String>): Unit
}
