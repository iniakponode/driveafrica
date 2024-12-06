package com.uoa.core.apiServices.apiServiceModels.drivingTipModels

// DrivingTipCreate.kt
data class DrivingTipCreate(
    val title: String,
    val meaning: String,
    val penalty: String,
    val fine: String,
    val law: String,
    val hostility: String,
    val summary_tip: String,
    val sync: Boolean,
    val date: String, // ISO 8601 format
    val profile_id: String, // UUID as String
    val llm: String
)
