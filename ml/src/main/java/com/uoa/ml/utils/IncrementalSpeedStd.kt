package com.uoa.ml.utils

import javax.inject.Inject
import kotlin.math.sqrt

// IncrementalSpeedStd.kt
class IncrementalSpeedStd @Inject constructor() {
    private var count = 0
    private var mean = 0.0
    private var m2 = 0.0

    fun addSpeed(speed: Float) {
        if (!speed.isFinite() || speed < 0f) {
            return
        }
        count++
        val delta = speed - mean
        mean += delta / count
        val delta2 = speed - mean
        m2 += delta * delta2
    }

    fun getStd(): Float {
        if (count < 2) {
            return 0.0f
        }
        val variance = m2 / (count - 1)
        return sqrt(variance).toFloat()
    }

    fun reset() {
        count = 0
        mean = 0.0
        m2 = 0.0
    }
}
