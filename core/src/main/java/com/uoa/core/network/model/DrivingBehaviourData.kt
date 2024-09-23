package com.uoa.core.network.model

import com.google.firebase.dataconnect.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

data class DrivingBehaviourData(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tripId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val locationId: UUID?,
    @Serializable
    val behaviorType: String,
    @Serializable
    val severity: Float,
    @Serializable
    val timestamp: Long,
    @Serializable
    val date: Date,
    @Serializable
    val synced: Boolean=false,
    @Serializable
    val causes: List<CauseData>
)
