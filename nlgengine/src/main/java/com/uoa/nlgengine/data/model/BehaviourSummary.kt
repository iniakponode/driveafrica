package com.uoa.nlgengine.data.model

import java.time.LocalDate

data class BehaviourSummary(
    val date: LocalDate,
    val location: String,
    val hour: Int,
    val totalBehaviors: Int,
    val behaviorCounts: Map<String, Int>,
    val mostFrequentBehavior: String,
    val alcoholInfluenceCount: Int
)
