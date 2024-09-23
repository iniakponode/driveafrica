package com.uoa.nlgengine.data.model

import java.time.LocalDate

data class LocationDateHourKey(
    val locationName: String,
    val date: LocalDate,
    val hour: Int
)

