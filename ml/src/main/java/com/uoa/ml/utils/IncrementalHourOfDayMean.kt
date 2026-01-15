package com.uoa.ml.utils

import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Named

class IncrementalHourOfDayMean @Inject constructor(
    @Named("TrainingTimeZone") private val trainingTimeZone: TimeZone
) {
    private var sumHours = 0.0
    private var count = 0
    private val calendar = Calendar.getInstance(trainingTimeZone)

    fun addTimestamp(timestamp: Long) {
        calendar.timeInMillis = timestamp
        val hour = calendar.get(Calendar.HOUR_OF_DAY).toDouble()
        sumHours += hour
        count++
    }

    fun getMean(): Float {
        if (count == 0) {
            return 0.0f
        }
        return (sumHours / count).toFloat()
    }

    fun reset() {
        sumHours = 0.0
        count = 0
    }
}
