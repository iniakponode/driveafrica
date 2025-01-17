package com.uoa.core.apiServices.models.locationModels

import java.util.UUID

// LocationCreate.kt
data class LocationCreate(
    val id: UUID,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val date: String, // ISO 8601 format
    val altitude: Double,
    val speed: Double,
    val distance: Double,
    val sync: Boolean = false
)
