package com.uoa.core.apiServices.apiServiceModels.locationModels

// LocationResponse.kt
data class LocationResponse(
    val id: String, // UUID as String
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val date: String, // ISO 8601 format
    val altitude: Double,
    val speed: Double,
    val distance: Double,
    val sync: Boolean
)
