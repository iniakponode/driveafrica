package com.uoa.core.apiServices.models.rawSensorModel

// RawSensorDataCreate.kt
data class RawSensorDataCreate(
    val sensor_type: Int,
    val sensor_type_name: String,
    val values: List<Double>,
    val timestamp: Long,
    val date: String, // ISO 8601 format
    val accuracy: Int,
    val location_id: String,
    val trip_id: String,
    val sync: Boolean
)
