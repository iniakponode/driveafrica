package com.uoa.core.apiServices.models.rawSensorModels

import java.util.UUID

// RawSensorDataResponse.kt
data class RawSensorDataResponse(
    val id: UUID,
    val sensor_type: Int,
    val sensor_type_name: String,
    val values: List<Double>,
    val timestamp: Long,
    val date: String,
    val accuracy: Int,
    val location_id: UUID,
    val trip_id: UUID,
    val sync: Boolean
)
