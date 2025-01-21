package com.uoa.core.apiServices.models.tripModels

import java.util.UUID

// TripResponse.kt
data class TripResponse(
    val id: UUID,
    val driverProfileId: UUID,
    val start_date: String,
    val end_date: String?,
    val start_time: Long,
    val end_time: Long?,
    val synced: Boolean
)
