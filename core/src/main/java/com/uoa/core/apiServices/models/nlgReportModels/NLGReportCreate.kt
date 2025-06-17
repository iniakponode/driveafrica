package com.uoa.core.apiServices.models.nlgReportModels

import java.time.LocalDateTime
import java.util.UUID

// NLGReportCreate.kt
data class NLGReportCreate(
    val id: UUID,
    val driverProfileId: UUID, // UUID as String
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val report_text: String,
    val generated_at: String, // ISO 8601 format (e.g., "2023-10-12T10:15:30")
    val sync: Boolean = false
)
