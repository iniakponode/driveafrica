package com.uoa.core.apiServices.models.tripSummaryModels

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class TripSummaryCreate(
    @field:SerializedName("tripId")
    val tripId: UUID,
    @field:SerializedName("driverProfileId")
    val driverProfileId: UUID,
    @field:SerializedName("startTime")
    val startTime: Long,
    @field:SerializedName("endTime")
    val endTime: Long,
    @field:SerializedName("startDate")
    val startDate: String,
    @field:SerializedName("endDate")
    val endDate: String,
    @field:SerializedName("distanceMeters")
    val distanceMeters: Double,
    @field:SerializedName("durationSeconds")
    val durationSeconds: Long,
    @field:SerializedName("unsafeBehaviourCounts")
    val unsafeBehaviourCounts: Map<String, Int>,
    @field:SerializedName("classificationLabel")
    val classificationLabel: String,
    @field:SerializedName("alcoholProbability")
    val alcoholProbability: Float?,
    @field:SerializedName("sync")
    val sync: Boolean = true
)
