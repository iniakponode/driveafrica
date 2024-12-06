package com.uoa.nlgengine.data.model

import java.time.LocalDate
import java.time.LocalTime

data class BehaviourOccurrence(
    val date: LocalDate,
    val time: LocalTime,
    val roadName: String
)
