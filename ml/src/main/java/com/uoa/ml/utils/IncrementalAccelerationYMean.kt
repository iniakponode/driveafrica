package com.uoa.ml.utils

import android.util.Log
import com.uoa.core.mlclassifier.MinMaxValuesLoader
import javax.inject.Inject

// IncrementalAccelerationYMean.kt
class IncrementalAccelerationYMean @Inject constructor(
    private val minMaxValuesLoader: MinMaxValuesLoader
) {
    private var sumAccelerationY = 0.0
    private var count = 0

    fun addAccelerationY(value: Float) {
        sumAccelerationY += value
        count++
    }

    fun getNormalizedMean(): Float {
        if (count == 0) {
            return 0.0f
        }

        val meanAccelY = (sumAccelerationY / count).toFloat()
        val minValue = minMaxValuesLoader.getMin("accelerationYOriginal_mean") ?: -10f
        val maxValue = minMaxValuesLoader.getMax("accelerationYOriginal_mean") ?: 10f
        val range = maxValue - minValue

        val normalizedMeanAccelY = if (range != 0f) {
            (meanAccelY - minValue) / range
        } else {
            0.0f
        }

        return normalizedMeanAccelY.coerceIn(0.0f, 1.0f)
    }

    fun reset() {
        sumAccelerationY = 0.0
        count = 0
    }
}
