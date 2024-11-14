package com.uoa.core.apiServices.models.rawSensorModel

// RawSensorDataResponse.kt
data class RawSensorDataResponse(
    val id: String,
    val sensor_type: Int,
    val sensor_type_name: String,
    val values: List<Double>,
    val timestamp: Long,
    val date: String,
    val accuracy: Int,
    val location_id: String,
    val trip_id: String,
    val sync: Boolean
)
