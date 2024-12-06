package com.uoa.core.apiServices.models.nlgReport

// NLGReportCreate.kt
data class NLGReportCreate(
    val driver_profile_id: String, // UUID as String
    val report_text: String,
    val generated_at: String, // ISO 8601 format
    val synced: Boolean = false
)
