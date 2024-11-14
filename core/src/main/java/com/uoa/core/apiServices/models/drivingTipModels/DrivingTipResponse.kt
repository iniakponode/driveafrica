package com.uoa.core.apiServices.apiServiceModels.drivingTipModels

// DrivingTipResponse.kt
data class DrivingTipResponse(
    val tip_id: String,
    val title: String,
    val meaning: String,
    val penalty: String,
    val fine: String,
    val law: String,
    val hostility: String,
    val summary_tip: String,
    val sync: Boolean,
    val date: String,
    val profile_id: String, // UUID as String
    val llm: String
)
