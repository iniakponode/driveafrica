package com.uoa.dbda.data.model

import java.util.UUID

data class UnsafeBehaviourModel(
    val id: UUID,
    val tripId: UUID,
    val locationId: UUID,
    val behaviorType: String,
    val severity: Float,
    val timestamp: Long,
    val synced: Boolean=false,
    val cause: String,
)
