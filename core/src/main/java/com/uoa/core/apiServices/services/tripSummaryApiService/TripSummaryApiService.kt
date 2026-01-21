package com.uoa.core.apiServices.services.tripSummaryApiService

import com.uoa.core.apiServices.models.tripSummaryModels.TripSummaryCreate
import retrofit2.http.Body
import retrofit2.http.POST

interface TripSummaryApiService {

    @POST("/api/trip_summaries/")
    suspend fun createTripSummary(@Body summary: TripSummaryCreate): TripSummaryCreate

    @POST("/api/trip_summaries/batch_create")
    suspend fun batchCreateTripSummaries(
        @Body summaries: List<TripSummaryCreate>
    ): Map<String, Int>
}
