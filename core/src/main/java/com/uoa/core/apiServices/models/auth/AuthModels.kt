package com.uoa.core.apiServices.models.auth

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val driverProfileId: String,
    val email: String,
    val password: String,
    val sync: Boolean = true
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("driver_profile_id")
    val driverProfileId: String,
    val email: String
)
