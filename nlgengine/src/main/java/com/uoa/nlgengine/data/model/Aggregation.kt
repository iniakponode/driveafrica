package com.uoa.nlgengine.data.model

enum class AggregationLevel {
    DAILY, WEEKLY, MONTHLY
}

fun getAggregationLevel(customPeriodDuration: Long): AggregationLevel {
    return when {
        customPeriodDuration <= 7 -> AggregationLevel.DAILY
        customPeriodDuration <= 30 -> AggregationLevel.WEEKLY
        else -> AggregationLevel.MONTHLY
    }
}