package com.uoa.core.apiServices.services.tripFeatureStateApiService

import com.uoa.core.apiServices.models.tripFeatureModels.TripFeatureStateCreate
import retrofit2.http.Body
import retrofit2.http.POST

interface TripFeatureStateApiService {

    @POST("/api/trip_feature_states/")
    suspend fun createTripFeatureState(
        @Body state: TripFeatureStateCreate
    ): TripFeatureStateCreate

    @POST("/api/trip_feature_states/batch_create")
    suspend fun batchCreateTripFeatureStates(
        @Body states: List<TripFeatureStateCreate>
    ): Map<String, Int>
}
