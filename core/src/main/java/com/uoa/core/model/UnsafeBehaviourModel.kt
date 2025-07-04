package com.uoa.core.model

import java.util.Date
import java.util.UUID

data class UnsafeBehaviourModel(
    val id: UUID,
    val tripId: UUID,
    val driverProfileId: UUID,
    val locationId: UUID?,
    val behaviorType: String,
    val severity: Float,
    val timestamp: Long,
    val date: Date,
    val updatedAt:Date?,
    val updated:Boolean=false,
    val processed: Boolean= false,
    val sync: Boolean=false,
)
