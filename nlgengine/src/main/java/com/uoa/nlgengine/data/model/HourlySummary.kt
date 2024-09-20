package com.uoa.nlgengine.data.model

import java.time.LocalDate

data class HourlySummary(
    val date: LocalDate,
    val hour: Int,
    val totalBehaviors: Int,
    val behaviorCounts: Map<String, Int>,
    val mostFrequentBehavior: String,
    val alcoholInfluenceCount: Int
)
