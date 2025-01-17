package com.uoa.core.model

import java.time.LocalDate
import java.time.LocalTime

data class BehaviourOccurrence(
    val date: LocalDate,
    val time: LocalTime,
    val roadName: String
)
