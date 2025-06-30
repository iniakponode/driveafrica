package com.uoa.core.apiServices.models.tripModels

import com.google.gson.annotations.SerializedName
import java.util.UUID

// TripResponse.kt
data class TripResponse(
    val id: UUID,
    val driverProfileId: UUID,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String?,
    @SerializedName("startTime") val startTime: Long,
    @SerializedName("endTime") val endTime: Long?,
    val synced: Boolean,
    val influence: String? = null
)
