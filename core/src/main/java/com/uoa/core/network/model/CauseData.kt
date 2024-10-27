package com.uoa.core.network.model

import com.google.firebase.dataconnect.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

data class CauseData(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable
    val name: String,
    @Serializable(with = UUIDSerializer::class)
    val unsafeBehaviourId: UUID,
    @Serializable
    val description: String,
    @Serializable
    val createdAt: String,
    @Serializable
    val updatedAt: String
)
