package com.uoa.core.apiServices.models.tripModel

// TripResponse.kt
data class TripResponse(
    val id: String,
    val driver_profile_id: String,
    val start_date: String,
    val end_date: String?,
    val start_time: Long,
    val end_time: Long?,
    val synced: Boolean
)
