package com.uoa.core.model

import java.util.UUID

data class Cause(
    val id: UUID,
    val name: String,
    val unsafeBehaviourId: UUID,
    val influence: Boolean?,
    val createdAt: String,
    val updatedAt: String?
)
