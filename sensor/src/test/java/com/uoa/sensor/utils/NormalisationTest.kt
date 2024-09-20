package com.uoa.sensor.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow
import kotlin.math.sqrt

class NormalizeUtilsTest {

    @Test
    fun testMinMaxNormalize() {
        val data = listOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
        val expected = listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)

        val result = NormalizeUtils.minMaxNormalize(data)

        assertEquals(expected, result)
    }

    @Test
    fun testMinMaxNormalizeWithSameValues() {
        val data = listOf(5.0f, 5.0f, 5.0f, 5.0f, 5.0f)
        val expected = listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)

        val result = NormalizeUtils.minMaxNormalize(data)

        assertEquals(expected, result)
    }

    @Test
    fun testZScoreNormalize() {
        val data = listOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
        val mean = data.average().toFloat()
        val stdDev = sqrt(data.map { (it - mean).toDouble().pow(2) }.average()).toFloat()
        val expected = data.map { (it - mean) / stdDev }

        val result = NormalizeUtils.zScoreNormalize(data)

        assertEquals(expected, result)
    }

    @Test
    fun testZScoreNormalizeWithSameValues() {
        val data = listOf(2.0f, 2.0f, 2.0f, 2.0f, 2.0f)
        val expected = listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)

        val result = NormalizeUtils.zScoreNormalize(data)

        assertEquals(expected, result)
    }
}