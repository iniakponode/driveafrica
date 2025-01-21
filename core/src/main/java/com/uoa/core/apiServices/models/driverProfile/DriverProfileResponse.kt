package com.uoa.core.apiServices.models.driverProfile

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class DriverProfileResponse(
    @SerializedName("driver_profile_id")
    val driverProfileId: UUID, // Should be parsed from String in JSON
    val email: String,
    val sync: Boolean
)
