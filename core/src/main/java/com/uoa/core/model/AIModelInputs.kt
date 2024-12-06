package com.uoa.core.model

import kotlinx.datetime.LocalDate
import java.util.Date
import java.util.UUID

data class AIModelInputs(
    val id: UUID,
    val tripId: UUID,
    val timestamp: Long,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val date: Date?,
    val hourOfDayMean: Double,
    val dayOfWeekMean: Float,
    val speedStd: Float,
    val courseStd: Float,
    val accelerationYOriginalMean: Float
)
