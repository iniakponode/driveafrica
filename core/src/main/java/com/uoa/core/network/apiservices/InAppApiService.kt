package com.uoa.core.network.apiservices

import com.uoa.core.network.model.DrivingBehaviourResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.UUID

interface InAppApiService {

    @GET("drivingBehaviour/{id}")
    fun getDrivingBehaviour(@Path("id") id: UUID): Call<DrivingBehaviourResponse>

    // Add other endpoints as needed
}