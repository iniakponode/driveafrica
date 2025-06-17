package com.uoa.core.apiServices.models.tripModels

import com.google.gson.annotations.SerializedName
import java.util.UUID

// TripCreate.kt
data class TripCreate(
    val id: UUID,
    val driverProfileId: UUID?,
    @SerializedName("start_date")
    val start_date: String?=null, // ISO 8601 format
    @SerializedName("end_date")
    val end_date: String?=null, // Optional
    val start_time: Long?=null,
    val end_time: Long?=null, // Optional
    val sync: Boolean,
    val influence: String
)
