package com.uoa.ml.utils

import android.util.Log
import com.uoa.core.mlclassifier.MinMaxValuesLoader
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Named

class IncrementalHourOfDayMean @Inject constructor(
    @Named("TrainingTimeZone") private val trainingTimeZone: TimeZone,
    private val minMaxValuesLoader: MinMaxValuesLoader
) {
    private var sumHours = 0.0
    private var count = 0
    private val calendar = Calendar.getInstance(trainingTimeZone)

    fun addTimestamp(timestamp: Long) {
        calendar.timeInMillis = timestamp
        val hour = calendar.get(Calendar.HOUR_OF_DAY).toDouble()
        sumHours += hour
        count++
        Log.d("Utils", "Timestamp: $timestamp -> Hour: $hour")
    }

    fun getNormalizedMean(minMaxValuesLoader: MinMaxValuesLoader): Float {
        if (count == 0) {
            Log.w("Utils", "No timestamps provided for hour of day mean extraction.")
            return 0.0f
        }

        val meanHour = (sumHours / count).toFloat()
        Log.d("Utils", "Mean Hour of Day: $meanHour")

        val minValue = minMaxValuesLoader.getMin("hour_of_day_mean") ?: 0f
        val maxValue = minMaxValuesLoader.getMax("hour_of_day_mean") ?: 23f
        val range = maxValue - minValue

        val normalizedHourMean = if (range != 0f) {
            (meanHour - minValue) / range
        } else {
            Log.w("Utils", "Range for hour_of_day_mean is zero. Defaulting normalized value to 0.0f")
            0.0f
        }

        Log.d("Utils", "Normalized Hour of Day Mean: $normalizedHourMean")
        return normalizedHourMean.coerceIn(0.0f, 1.0f)
    }
}
