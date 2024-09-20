package com.uoa.core.model

import java.util.Date
import java.util.UUID

data class AIModelInputs(
    val id: UUID,
    val tripId: UUID,
    val timestamp: Long,
    val date: Date,
    val hourOfDayMean: Double,
    val dayOfWeekMean: Float,
    val speedStd: Float,
    val courseStd: Float,
    val accelerationYOriginalMean: Float
)
