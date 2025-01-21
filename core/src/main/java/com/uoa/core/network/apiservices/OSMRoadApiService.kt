package com.uoa.core.network.apiservices

import com.uoa.core.network.model.nominatim.ReverseGeocodeResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OSMRoadApiService {
    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "jsonv2"
    ): ReverseGeocodeResponse
}
