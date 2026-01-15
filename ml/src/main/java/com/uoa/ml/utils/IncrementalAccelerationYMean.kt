package com.uoa.ml.utils

import javax.inject.Inject

// IncrementalAccelerationYMean.kt
class IncrementalAccelerationYMean @Inject constructor() {
    private var sumAccelerationY = 0.0
    private var count = 0

    fun addAccelerationY(value: Float) {
        if (!value.isFinite()) {
            return
        }
        sumAccelerationY += value
        count++
    }

    fun getMean(): Float {
        if (count == 0) {
            return 0.0f
        }
        return (sumAccelerationY / count).toFloat()
    }

    fun reset() {
        sumAccelerationY = 0.0
        count = 0
    }
}
