package com.uoa.core.network.model

import com.google.firebase.dataconnect.serializers.UUIDSerializer
import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

@Keep
data class DrivingBehaviourData(
    @Serializable(with = UUIDSerializer::class)
    @field:SerializedName("id")
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    @field:SerializedName("tripId")
    val tripId: UUID,
    @Serializable(with = UUIDSerializer::class)
    @field:SerializedName("locationId")
    val locationId: UUID?,
    @Serializable
    @field:SerializedName("behaviorType")
    val behaviorType: String,
    @Serializable
    @field:SerializedName("severity")
    val severity: Float,
    @Serializable
    @field:SerializedName("timestamp")
    val timestamp: Long,
    @Serializable
    @field:SerializedName("date")
    val date: Date,
    @Serializable
    @field:SerializedName("synced")
    val synced: Boolean = false,
    @Serializable
    @field:SerializedName("causes")
    val causes: List<CauseData>
)
