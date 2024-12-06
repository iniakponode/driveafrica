package com.uoa.core.model

import java.util.UUID

data class Road (
    val id: UUID,
    val name: String,
    val roadType: String,
    val speedLimit: Int,
    val latitude: Double,
    val longitude: Double
)
