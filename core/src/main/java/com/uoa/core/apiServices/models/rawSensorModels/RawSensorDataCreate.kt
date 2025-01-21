package com.uoa.core.apiServices.models.rawSensorModels

import java.util.Date
import java.util.UUID

// RawSensorDataCreate.kt
data class RawSensorDataCreate(
    val id: UUID,
    val sensor_type: Int,
    val sensor_type_name: String,
    val values: List<Float>,
    val timestamp: Long,
    val date: String?, // ISO 8601 format
    val accuracy: Int,
    val location_id: UUID?,
    val driverProfileId: UUID,
    val trip_id: UUID,
    val sync: Boolean
)
