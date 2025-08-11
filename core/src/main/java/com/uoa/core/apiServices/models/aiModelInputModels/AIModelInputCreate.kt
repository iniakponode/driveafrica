package com.uoa.core.apiServices.models.aiModelInputModels

import com.google.gson.annotations.SerializedName
import java.util.UUID

// AIModelInputCreate.kt
data class AIModelInputCreate(
    val id: UUID,
    val trip_id: UUID,
    @SerializedName("driverProfileId")
    val driverProfileId: UUID,
    val timestamp: String, // ISO 8601 format
    val start_time: Long,
    val date: String, // ISO 8601 format
    val hour_of_day_mean: Double,
    val day_of_week_mean: Double,
    val speed_std: Double,
    val course_std: Double,
    val acceleration_y_original_mean: Double,
    val sync: Boolean
)
