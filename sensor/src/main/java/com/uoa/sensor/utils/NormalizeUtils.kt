package com.uoa.sensor.utils

import kotlin.math.pow
import kotlin.math.sqrt

object NormalizeUtils {

    fun minMaxNormalize(data: List<Float>): List<Float> {
        val min = data.minOrNull() ?: 0.0f  // Default to 0 if all values are null
        val max = data.maxOrNull() ?: 0.0f  // Default to 0 if all values are null

        if (min == max) {
            return data.map { 0.0f } // Handle case of all values being the same
        } else {
            return data.map { (it - min) / (max - min) }
        }
    }

    fun zScoreNormalize(data: List<Float>): List<Float> {
        val mean = data.average().toFloat()
        val variance = data.map { (it - mean).toDouble().pow(2) }.average()

        // Check for zero variance
        if (variance == 0.0) {
            return data.map { 0.0f } // Or any other appropriate handling
        }

        val stdDev = sqrt(variance).toFloat()
        return data.map { (it - mean) / stdDev }
    }

}