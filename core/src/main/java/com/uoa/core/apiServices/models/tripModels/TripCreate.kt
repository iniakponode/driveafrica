package com.uoa.core.apiServices.models.tripModel

// TripCreate.kt
data class TripCreate(
    val driver_profile_id: String, // UUID as String
    val start_date: String, // ISO 8601 format
    val end_date: String?, // Optional
    val start_time: Long,
    val end_time: Long?, // Optional
    val synced: Boolean
)
