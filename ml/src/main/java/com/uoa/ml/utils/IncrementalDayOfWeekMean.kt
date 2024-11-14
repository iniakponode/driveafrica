package com.uoa.ml.utils

import android.util.Log
import com.uoa.core.mlclassifier.MinMaxValuesLoader
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Named

class IncrementalDayOfWeekMean(@Named("TrainingTimeZone") private val trainingTimeZone: TimeZone,
                               private val minMaxValuesLoader: MinMaxValuesLoader) {
    private var sumDays = 0.0
    private var count = 0
    private val calendar = Calendar.getInstance(trainingTimeZone)

    fun addTimestamp(timestamp: Long) {
        calendar.timeInMillis = timestamp
        val day = (calendar.get(Calendar.DAY_OF_WEEK) - 1).toDouble() // Adjust to 0-6
        sumDays += day
        count++
        Log.d("Utils", "Timestamp: $timestamp -> Day of Week: $day")
    }

    fun getNormalizedMean(minMaxValuesLoader: MinMaxValuesLoader): Float {
        if (count == 0) {
            Log.w("Utils", "No timestamps provided for day of week mean extraction.")
            return 0.0f
        }

        val meanDay = (sumDays / count).toFloat()
        Log.d("Utils", "Mean Day of Week: $meanDay")

        val minValue = minMaxValuesLoader.getMin("day_of_week_mean") ?: 0f
        val maxValue = minMaxValuesLoader.getMax("day_of_week_mean") ?: 6f
        val range = maxValue - minValue

        val normalizedDayMean = if (range != 0f) {
            (meanDay - minValue) / range
        } else {
            Log.w("Utils", "Range for day_of_week_mean is zero. Defaulting normalized value to 0.0f")
            0.0f
        }

        Log.d("Utils", "Normalized Day of Week Mean: $normalizedDayMean")
        return normalizedDayMean.coerceIn(0.0f, 1.0f)
    }
}
