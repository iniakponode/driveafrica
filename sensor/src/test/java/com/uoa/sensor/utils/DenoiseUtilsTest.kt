package com.uoa.sensor.utils


import org.junit.Assert.assertEquals
import org.junit.Test

class DenoiseUtilsTest {

    @Test
    fun testMedianFilter() {
        val data = listOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
        val windowSize = 3
        val expected = listOf(1.5f, 2.0f, 3.0f, 4.0f, 4.5f)

        val result = DenoiseUtils.medianFilter(data, windowSize)

        assertEquals(expected, result)
    }

    @Test
    fun testMedianFilterWithEdgeCases() {
        val data = listOf(1.0f, 2.0f)
        val windowSize = 3
        val expected = listOf(1.5f, 1.5f)

        val result = DenoiseUtils.medianFilter(data, windowSize)

        assertEquals(expected, result)
    }

    @Test
    fun testMedianFilterWithOddWindowSize() {
        val data = listOf(1.0f, 3.0f, 2.0f, 4.0f, 5.0f)
        val windowSize = 3
        val expected = listOf(2.0f, 2.0f, 3.0f, 4.0f, 4.5f)

        val result = DenoiseUtils.medianFilter(data, windowSize)

        assertEquals(expected, result)
    }

    @Test
    fun testMedianFilterWithEvenWindowSize() {
        val data = listOf(1.0f, 3.0f, 2.0f, 4.0f, 5.0f)
        val windowSize = 4
        val expected = listOf(2.0f, 2.5f, 3.0f, 3.5f, 4.0f)

        val result = DenoiseUtils.medianFilter(data, windowSize)

        assertEquals(expected, result)
    }

    @Test
    fun testMovingAverageFilterWithSingleElement() {
        val data = listOf(1.0f)
        val windowSize = 3
        val expected = listOf(1.0f)

        val result = DenoiseUtils.movingAverageFilter(data, windowSize)

        assertEquals(expected, result)
    }

    @Test
    fun testMovingAverageFilterWithEdgeCases() {
        val data = listOf(1.0f, 2.0f)
        val windowSize = 3
        val expected = listOf(1.5f, 1.5f)

        val result = DenoiseUtils.movingAverageFilter(data, windowSize)

        assertEquals(expected, result)
    }

    @Test
    fun testMovingAverageFilter() {
        val data = listOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
        val windowSize = 3
        val expected = listOf(1.5f, 2.0f, 3.0f, 4.0f, 4.5f)

        val result = DenoiseUtils.movingAverageFilter(data, windowSize)

        assertEquals(expected, result)
    }
}