package com.uoa.core.apiServices.models.aiModelInputModels

import java.util.UUID

// AIModelInputResponse.kt
data class AIModelInputResponse(
    val id: UUID,
    val trip_id: UUID,
    val timestamp: String,
    val date: String,
    val hour_of_day_mean: Double,
    val day_of_week_mean: Double,
    val speed_std: Double,
    val course_std: Double,
    val acceleration_y_original_mean: Double,
    val sync: Boolean
)
