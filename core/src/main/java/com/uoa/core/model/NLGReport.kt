package com.uoa.core.model

import java.util.Date
import java.util.UUID

data class NLGReport(
    val id: UUID,
    val userId: UUID,
    val tripId: UUID?=null,
    val reportText: String,
    val startDate: java.time.LocalDateTime?=null,
    val endDate: java.time.LocalDateTime?=null,
    val createdDate: java.time.LocalDateTime,
    val sync: Boolean,
)
