package com.uoa.core.apiServices.services.fleetApiService

import com.uoa.core.apiServices.models.auth.FleetStatusResponse
import com.uoa.core.apiServices.models.auth.InviteCodeValidationRequest
import com.uoa.core.apiServices.models.auth.JoinFleetRequest
import com.uoa.core.apiServices.models.auth.JoinFleetResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

interface DriverFleetApiService {
    @POST("/api/driver-join/validate-code")
    suspend fun validateInviteCode(@Body request: InviteCodeValidationRequest): Response<Unit>

    @POST("/api/driver/join-fleet")
    suspend fun joinFleet(@Body request: JoinFleetRequest): JoinFleetResponse

    @POST("/api/driver-join/join-with-code")
    suspend fun joinWithCode(@Body request: JoinFleetRequest): FleetStatusResponse

    @GET("/api/driver/fleet-status")
    suspend fun getFleetStatus(): FleetStatusResponse

    @DELETE("/api/driver/join-request")
    suspend fun cancelJoinRequest()
}
