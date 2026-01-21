package com.uoa.core.apiServices.models.tripSummaryModels

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class TripSummaryBehaviourCreate(
    @SerializedName("tripId")
    val tripId: UUID,
    @SerializedName("behaviourType")
    val behaviourType: String,
    @SerializedName("count")
    val count: Int
)
