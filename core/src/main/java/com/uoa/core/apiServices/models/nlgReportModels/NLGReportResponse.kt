package com.uoa.core.apiServices.models.nlgReport

// NLGReportResponse.kt
data class NLGReportResponse(
    val id: String, // UUID as String
    val driver_profile_id: String,
    val report_text: String,
    val generated_at: String, // ISO 8601 format
    val synced: Boolean
)