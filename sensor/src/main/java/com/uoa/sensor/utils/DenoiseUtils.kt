package com.uoa.sensor.utils

object DenoiseUtils {

    fun movingAverageFilter(data: List<Float>, windowSize: Int): List<Float> {
        val result = mutableListOf<Float>()
        for (i in data.indices) {
            val start = maxOf(0, i - windowSize / 2)
            val end = minOf(data.size - 1, i + windowSize / 2)
            val window = data.subList(start, end + 1)
            result.add(window.average().toFloat())
        }
        return result
    }

    fun medianFilter(data: List<Float>, windowSize: Int): List<Float> {
        val result = mutableListOf<Float>()
        val halfWindow = windowSize / 2
        for (i in data.indices) {
            val start = maxOf(0, i - halfWindow)
            val end = minOf(data.size, i + halfWindow + 1)
            val window = data.subList(start, end).sorted()
            val median = if (window.size % 2 == 0) {
                (window[window.size / 2 - 1] + window[window.size / 2]) / 2
            } else {
                window[window.size / 2]
            }
            result.add(median)
        }
        return result
    }

}