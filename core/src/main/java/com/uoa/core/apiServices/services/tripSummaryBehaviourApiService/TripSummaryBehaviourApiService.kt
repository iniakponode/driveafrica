package com.uoa.core.apiServices.services.tripSummaryBehaviourApiService

import com.uoa.core.apiServices.models.tripSummaryModels.TripSummaryBehaviourCreate
import retrofit2.http.Body
import retrofit2.http.POST

interface TripSummaryBehaviourApiService {

    @POST("/api/trip_summary_behaviours/")
    suspend fun createTripSummaryBehaviour(
        @Body behaviour: TripSummaryBehaviourCreate
    ): TripSummaryBehaviourCreate

    @POST("/api/trip_summary_behaviours/batch_create")
    suspend fun batchCreateTripSummaryBehaviours(
        @Body behaviours: List<TripSummaryBehaviourCreate>
    ): Map<String, Int>
}
