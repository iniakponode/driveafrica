package com.uoa.core.apiServices.models.unsafeBehaviourModels

import java.util.UUID

// UnsafeBehaviourResponse.kt
data class UnsafeBehaviourResponse(
    val id: UUID,
    val trip_id: UUID?=null,
    val location_id: UUID?=null,
    val driverProfileId: UUID,
    val behaviour_type: String,
    val severity: Double,
    val timestamp: Long,
    val date: String,
)
