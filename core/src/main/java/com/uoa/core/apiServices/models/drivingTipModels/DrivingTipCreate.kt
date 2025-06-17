package com.uoa.core.apiServices.models.drivingTipModels

import java.util.UUID

// DrivingTipCreate.kt
data class DrivingTipCreate(
    val tip_id: UUID,
    val title: String,
    val meaning: String,
    val penalty: String,
    val fine: String,
    val law: String,
    val hostility: String,
    val summary_tip: String,
    val sync: Boolean,
    val date: String, // ISO 8601 format
    val profile_id: UUID, // UUID as String
    val llm: String
)
