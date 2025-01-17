package com.uoa.core.model

import java.time.LocalDate
import java.util.UUID

data class DrivingTip(
val tipId: UUID,
val driverProfileId: UUID,
val title: String,
val meaning: String?=null,
val penalty: String?=null,
val fine: String?=null,
val law: String?=null,
val hostility: String,
val summaryTip: String?=null,
val date:LocalDate,
val sync: Boolean = false,
val llm: String?=null,
)
