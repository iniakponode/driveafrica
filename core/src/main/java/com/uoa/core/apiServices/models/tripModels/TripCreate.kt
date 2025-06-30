package com.uoa.core.apiServices.models.tripModels

import com.google.gson.annotations.SerializedName
import java.util.UUID

// TripCreate.kt
data class TripCreate(
    val id: UUID,
    val driverProfileId: UUID?,
    @SerializedName("startDate")
    val startDate: String? = null, // ISO 8601 format
    @SerializedName("endDate")
    val endDate: String? = null, // Optional
    @SerializedName("startTime")
    val startTime: Long? = null,
    @SerializedName("endTime")
    val endTime: Long? = null, // Optional
    val sync: Boolean,
    val influence: String
)
