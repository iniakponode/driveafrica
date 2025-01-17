package com.uoa.core.model

import java.util.Date
import java.util.UUID

data class NLGReport(
    val id: UUID,
    val userId: UUID,
    val tripId: UUID?=null,
    val reportText: String,
    val startDate: java.time.LocalDate?=null,
    val endDate: java.time.LocalDate?=null,
    val createdDate: java.time.LocalDate,
    val synced: Boolean,
)
