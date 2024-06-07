package com.uoa.sensor.utils

object FilterUtils {

    fun lowPassFilter(input: FloatArray, output: FloatArray, alpha: Float): FloatArray {
        for (i in input.indices) {
            output[i] = alpha * input[i] + (1 - alpha) * output[i]
        }
        return output
    }

    fun highPassFilter(input: FloatArray, output: FloatArray, alpha: Float): FloatArray {
        for (i in input.indices) {
            output[i] = alpha * (output[i] + input[i] - (if (i > 0) input[i - 1] else 0f))
        }
        return output
    }

    fun removeOffsets(input: FloatArray, offsets: FloatArray): FloatArray {
        val adjustedValues = FloatArray(input.size)
        for (i in input.indices) {
            adjustedValues[i] = input[i] - offsets[i]
        }
        return adjustedValues
    }
}