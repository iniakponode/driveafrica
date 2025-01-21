package com.uoa.core.apiServices.models.aiModelInputModels

import java.util.UUID

// AIModelInputCreate.kt
data class AIModelInputCreate(
    val id: UUID,
    val trip_id: UUID,
    val driver_profile_id: UUID,
    val timestamp: String, // ISO 8601 format
    val startTimeStamp: String, // Iso 8601 format
    val endTimeStamp: String,
    val date: String, // ISO 8601 format
    val hour_of_day_mean: Double,
    val day_of_week_mean: Double,
    val speed_std: Double,
    val course_std: Double,
    val acceleration_y_original_mean: Double,
    val synced: Boolean
)
