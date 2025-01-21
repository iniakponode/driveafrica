package com.uoa.core.apiServices.models.drivingTipModels

import com.google.gson.annotations.SerializedName
import java.util.UUID

// DrivingTipResponse.kt
data class DrivingTipResponse(
    val id: UUID,
    val tip_id: UUID,
    val title: String,
    val meaning: String,
    val penalty: String,
    val fine: String,
    val law: String,
    val hostility: String,
    val summary_tip: String,
    val sync: Boolean,
    val date: String,
    val profile_id: UUID, // UUID as String
    val llm: String
)
