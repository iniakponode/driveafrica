package com.uoa.core.network.model

import com.google.firebase.dataconnect.serializers.UUIDSerializer
import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import java.util.UUID

@Keep
data class CauseData(
    @Serializable(with = UUIDSerializer::class)
    @field:SerializedName("id")
    val id: UUID,
    @Serializable
    @field:SerializedName("name")
    val name: String,
    @Serializable(with = UUIDSerializer::class)
    @field:SerializedName("unsafeBehaviourId")
    val unsafeBehaviourId: UUID,
    @Serializable
    @field:SerializedName("description")
    val description: String,
    @Serializable
    @field:SerializedName("createdAt")
    val createdAt: String,
    @Serializable
    @field:SerializedName("updatedAt")
    val updatedAt: String
)
