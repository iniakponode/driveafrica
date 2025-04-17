package com.uoa.core.apiServices.models.roadModels

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class RoadCreate(
    val id: UUID,
    val driverProfileId: UUID?,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("roadType")
    val roadType: String? = null,
    @SerializedName("speedLimit")
    val speedLimit: Int? = null,
    @SerializedName("latitude")
    val latitude: Double? = null,
    @SerializedName("longitude")
    val longitude: Double? = null,
    @SerializedName("radius")
    val radius: Double? = null,
    @SerializedName("sync")
    val sync: Boolean
)
