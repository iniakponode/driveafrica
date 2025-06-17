package com.uoa.core.apiServices.models.unsafeBehaviourModels

import java.util.UUID

// UnsafeBehaviourCreate.kt
data class UnsafeBehaviourCreate(
    val id:UUID,
    val trip_id: UUID?=null,
    val location_id: UUID?=null,
    val driverProfileId: UUID,
    val behaviour_type: String,
    val severity: Double,
    val timestamp: Long,
    val date: String,
    val sync: Boolean
)
