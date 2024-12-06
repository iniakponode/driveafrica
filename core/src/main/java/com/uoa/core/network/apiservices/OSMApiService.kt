package com.uoa.core.network.apiservices

import com.uoa.core.network.model.chatGPT.OSMResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Call

interface OSMApiService {
    @GET("reverse")
    suspend fun getReverseGeocoding(
        @Query("format") format: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("zoom") zoom: Int,
        @Query("addressdetails") addressdetails: Int
    ): OSMResponse
}
