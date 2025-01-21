package com.uoa.core.apiServices.models.driverProfile

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class DriverProfileCreate(
    val driverProfileId: UUID, // Should be sent as a String in JSON
    val email: String,
    val sync: Boolean = false
)
