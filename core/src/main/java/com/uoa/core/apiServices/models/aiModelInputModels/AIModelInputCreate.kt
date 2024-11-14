package com.uoa.core.apiServices.models.aiModelInput

// AIModelInputCreate.kt
data class AIModelInputCreate(
    val trip_id: String,
    val timestamp: String, // ISO 8601 format
    val date: String, // ISO 8601 format
    val hour_of_day_mean: Double,
    val day_of_week_mean: Double,
    val speed_std: Double,
    val course_std: Double,
    val acceleration_y_original_mean: Double,
    val synced: Boolean
)
