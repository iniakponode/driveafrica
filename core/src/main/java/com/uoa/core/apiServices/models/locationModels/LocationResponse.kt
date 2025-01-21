package com.uoa.core.apiServices.models.locationModels

import java.util.UUID

// LocationResponse.kt
data class LocationResponse(
    val id: UUID, // UUID as String
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val date: String, // ISO 8601 format
    val altitude: Double,
    val speed: Double,
    val speedLimit: Double,
    val distance: Double,
    val sync: Boolean
)
