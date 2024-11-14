package com.uoa.core.apiServices.models.unsafeBehaviour

// UnsafeBehaviourResponse.kt
data class UnsafeBehaviourResponse(
    val id: String,
    val trip_id: String,
    val location_id: String,
    val driver_profile_id: String,
    val behaviour_type: String,
    val severity: Double,
    val timestamp: Long,
    val date: String,
    val alcohol_influence: Boolean
)
