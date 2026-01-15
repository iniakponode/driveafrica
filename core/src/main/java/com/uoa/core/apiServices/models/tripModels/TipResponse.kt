package com.uoa.core.apiServices.models.tripModels

import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.JsonAdapter
import com.uoa.core.apiServices.FlexibleLongAdapter
import java.util.UUID

// TripResponse.kt
data class TripResponse(
    val id: UUID,
    val driverProfileId: UUID,
    @SerializedName(value = "startDate", alternate = ["start_date"])
    val startDate: String,
    @SerializedName(value = "endDate", alternate = ["end_date"])
    val endDate: String?,
    @SerializedName(value = "startTime", alternate = ["start_time"])
    @JsonAdapter(FlexibleLongAdapter::class)
    val startTime: Long?,
    @SerializedName(value = "endTime", alternate = ["end_time"])
    @JsonAdapter(FlexibleLongAdapter::class)
    val endTime: Long?,
    @SerializedName(value = "startTimeLocal", alternate = ["start_time_local"])
    val startTimeLocal: String? = null,
    @SerializedName(value = "endTimeLocal", alternate = ["end_time_local"])
    val endTimeLocal: String? = null,
    @SerializedName(value = "timeZoneId", alternate = ["time_zone_id"])
    val timeZoneId: String? = null,
    @SerializedName(value = "timeZoneOffsetMinutes", alternate = ["time_zone_offset_minutes"])
    val timeZoneOffsetMinutes: Int? = null,
    @SerializedName(value = "sync", alternate = ["synced"])
    val synced: Boolean,
    val influence: String? = null,
    @SerializedName(value = "userAlcoholResponse", alternate = ["user_alcohol_response"])
    val userAlcoholResponse: String? = null,
    @SerializedName(value = "alcoholProbability", alternate = ["alcohol_probability"])
    val alcoholProbability: Float? = null
)
