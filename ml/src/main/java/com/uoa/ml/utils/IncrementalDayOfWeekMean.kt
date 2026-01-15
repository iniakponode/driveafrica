package com.uoa.ml.utils

import java.util.Calendar
import java.util.TimeZone
import javax.inject.Named

class IncrementalDayOfWeekMean(
    @Named("TrainingTimeZone") private val trainingTimeZone: TimeZone
) {
    private var sumDays = 0.0
    private var count = 0
    private val calendar = Calendar.getInstance(trainingTimeZone)

    fun addTimestamp(timestamp: Long) {
        calendar.timeInMillis = timestamp
        val day = ((calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7).toDouble() // Monday=0..Sunday=6
        sumDays += day
        count++
    }

    fun getMean(): Float {
        if (count == 0) {
            return 0.0f
        }
        return (sumDays / count).toFloat()
    }

    fun reset() {
        sumDays = 0.0
        count = 0
    }
}
