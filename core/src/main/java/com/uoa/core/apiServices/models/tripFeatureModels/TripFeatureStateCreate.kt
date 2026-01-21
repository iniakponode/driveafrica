package com.uoa.core.apiServices.models.tripFeatureModels

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class TripFeatureStateCreate(
    @SerializedName("tripId")
    val tripId: UUID,
    @SerializedName("driverProfileId")
    val driverProfileId: UUID?,
    @SerializedName("accelCount")
    val accelCount: Int,
    @SerializedName("accelMean")
    val accelMean: Double,
    @SerializedName("speedCount")
    val speedCount: Int,
    @SerializedName("speedMean")
    val speedMean: Double,
    @SerializedName("speedM2")
    val speedM2: Double,
    @SerializedName("courseCount")
    val courseCount: Int,
    @SerializedName("courseMean")
    val courseMean: Double,
    @SerializedName("courseM2")
    val courseM2: Double,
    @SerializedName("lastLocationId")
    val lastLocationId: UUID?,
    @SerializedName("lastLatitude")
    val lastLatitude: Double?,
    @SerializedName("lastLongitude")
    val lastLongitude: Double?,
    @SerializedName("lastLocationTimestamp")
    val lastLocationTimestamp: Long?,
    @SerializedName("lastSensorTimestamp")
    val lastSensorTimestamp: Long?,
    @SerializedName("sync")
    val sync: Boolean = true
)
