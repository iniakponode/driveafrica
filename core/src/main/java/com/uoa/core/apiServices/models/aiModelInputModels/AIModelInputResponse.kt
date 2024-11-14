package com.uoa.core.apiServices.models.aiModelInput

// AIModelInputResponse.kt
data class AIModelInputResponse(
    val id: String,
    val trip_id: String,
    val timestamp: String,
    val date: String,
    val hour_of_day_mean: Double,
    val day_of_week_mean: Double,
    val speed_std: Double,
    val course_std: Double,
    val acceleration_y_original_mean: Double,
    val synced: Boolean
)
