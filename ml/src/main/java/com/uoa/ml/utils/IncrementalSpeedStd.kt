package com.uoa.ml.utils

import android.util.Log
import com.uoa.core.mlclassifier.MinMaxValuesLoader
import javax.inject.Inject
import kotlin.math.sqrt

// IncrementalSpeedStd.kt
class IncrementalSpeedStd @Inject constructor(
    private val minMaxValuesLoader: MinMaxValuesLoader
) {
    private var count = 0
    private var mean = 0.0
    private var m2 = 0.0

    fun addSpeed(speed: Float) {
        count++
        val delta = speed - mean
        mean += delta / count
        val delta2 = speed - mean
        m2 += delta * delta2
    }

    fun getNormalizedStd(): Float {
        if (count < 2) {
            return 0.0f
        }

        val variance = m2 / (count - 1)
        val speedStd = sqrt(variance).toFloat()
        val minValue = minMaxValuesLoader.getMin("speed_std") ?: 0f
        val maxValue = minMaxValuesLoader.getMax("speed_std") ?: 100f
        val range = maxValue - minValue

        val normalizedSpeedStd = if (range != 0f) {
            (speedStd - minValue) / range
        } else {
            0.0f
        }

        return normalizedSpeedStd.coerceIn(0.0f, 1.0f)
    }
}
