package com.uoa.core.apiServices.models.tripModels

import com.google.gson.annotations.SerializedName
import java.util.UUID

// TripCreate.kt
data class TripCreate(
    val id: UUID,
    val driverProfileId: UUID?,
    @SerializedName("startDate")
    val startDate: String? = null, // ISO 8601 format
    @SerializedName("start_date")
    val startDateSnake: String? = startDate,
    @SerializedName("endDate")
    val endDate: String? = null, // Optional
    @SerializedName("end_date")
    val endDateSnake: String? = endDate,
    @SerializedName("startTime")
    val startTime: Long? = null,
    @SerializedName("start_time")
    val startTimeSnake: Long? = startTime,
    @SerializedName("endTime")
    val endTime: Long? = null, // Optional
    @SerializedName("end_time")
    val endTimeSnake: Long? = endTime,
    @SerializedName("timeZoneId")
    val timeZoneId: String? = null,
    @SerializedName("time_zone_id")
    val timeZoneIdSnake: String? = timeZoneId,
    @SerializedName("timeZoneOffsetMinutes")
    val timeZoneOffsetMinutes: Int? = null,
    @SerializedName("time_zone_offset_minutes")
    val timeZoneOffsetMinutesSnake: Int? = timeZoneOffsetMinutes,
    val sync: Boolean,
    val influence: String?,
    @SerializedName("userAlcoholResponse")
    val userAlcoholResponse: String? = null,
    @SerializedName("user_alcohol_response")
    val userAlcoholResponseSnake: String? = userAlcoholResponse,
    @SerializedName("alcoholProbability")
    val alcoholProbability: Float? = null,
    @SerializedName("alcohol_probability")
    val alcoholProbabilitySnake: Float? = alcoholProbability
)
