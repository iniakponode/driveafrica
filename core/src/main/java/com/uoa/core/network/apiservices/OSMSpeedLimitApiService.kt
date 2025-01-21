package com.uoa.core.network.apiservices

import com.uoa.core.network.model.nominatim.OverpassResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OSMSpeedLimitApiService {
    @GET("interpreter")
    suspend fun fetchSpeedLimits(
        @Query("data") query: String
    ): OverpassResponse
}