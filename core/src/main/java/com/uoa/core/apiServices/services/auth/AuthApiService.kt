package com.uoa.core.apiServices.services.auth

import com.uoa.core.apiServices.models.auth.AuthResponse
import com.uoa.core.apiServices.models.auth.LoginRequest
import com.uoa.core.apiServices.models.auth.RegisterRequest
import com.uoa.core.apiServices.models.driverProfile.DriverProfileResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {

    @POST("/api/auth/driver/register")
    suspend fun registerDriver(@Body request: RegisterRequest): AuthResponse

    @POST("/api/auth/driver/login")
    suspend fun loginDriver(@Body request: LoginRequest): AuthResponse

    @GET("/api/auth/driver/me")
    suspend fun getCurrentDriver(): DriverProfileResponse
}
