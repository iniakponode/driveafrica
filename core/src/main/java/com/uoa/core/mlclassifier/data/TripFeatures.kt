package com.uoa.core.mlclassifier.data

data class TripFeatures(
    val hourOfDayMean: Float,
    val dayOfWeekMean: Float,
    val speedStd: Float,
    val courseStd: Float,
    val accelerationYOriginalMean: Float
)
