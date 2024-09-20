package com.uoa.nlgengine.data.model

import java.util.UUID

data class UnsafeBehaviour(
    val id: UUID,
    val behaviorType: String,
    val date: String,
    val locationId: UUID?)
