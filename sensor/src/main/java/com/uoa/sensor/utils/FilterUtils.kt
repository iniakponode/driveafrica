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
            if (i == 0) {
                // Handle the first element differently to initialize the filter
                output[i] = alpha * input[i]
            } else {
                output[i] = alpha * (output[i - 1] + input[i] - input[i - 1])
            }
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