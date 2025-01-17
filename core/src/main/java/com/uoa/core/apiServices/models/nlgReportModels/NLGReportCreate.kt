package com.uoa.core.apiServices.models.nlgReportModels

import java.util.UUID

// NLGReportCreate.kt
data class NLGReportCreate(
    val id: UUID,
    val driverProfileId: UUID, // UUID as String
    val startDate: java.time.LocalDate,
    val endDate: java.time.LocalDate,
    val report_text: String,
    val generated_at: String, // ISO 8601 format (e.g., "2023-10-12T10:15:30")
    val synced: Boolean = false
)
