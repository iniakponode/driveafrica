package com.uoa.core.apiServices.models.roadModels

import java.util.UUID

data class RoadResponse(
    val id: UUID,
    val driverProfileId: UUID,
    val name: String,
    val roadType: String,
    val speedLimit: Int,
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    val sync: Boolean
)
