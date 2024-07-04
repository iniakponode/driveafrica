package com.uoa.dbda.util

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
        for (i in data.indices) {
            val start = maxOf(0, i - windowSize / 2)
            val end = minOf(data.size - 1, i + windowSize / 2)
            val window = data.subList(start, end + 1).sorted()
            result.add(window[window.size / 2])
        }
        return result
    }
}