package com.uoa.sensor.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class FilterUtilsTest {

    @Test
    fun testLowPassFilter() {
        val input = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
        val output = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        val alpha = 0.5f
        val expected = floatArrayOf(0.5f, 1.0f, 1.5f, 2.0f, 2.5f)

        val result = FilterUtils.lowPassFilter(input, output, alpha)

        assertArrayEquals(expected, result, 0.01f)
    }

    @Test
    fun testHighPassFilter() {
        val input = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
        val output = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        val alpha = 0.5f
        val expected = floatArrayOf(0.5f, 0.75f, 0.875f, 0.9375f, 0.96875f) // Corrected expected values

        val result = FilterUtils.highPassFilter(input, output, alpha)

        assertArrayEquals(expected, result, 0.01f)
    }

    @Test
    fun testRemoveOffsets() {
        val input = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
        val offsets = floatArrayOf(0.5f, 1.0f, 1.5f, 2.0f, 2.5f)
        val expected = floatArrayOf(0.5f, 1.0f, 1.5f, 2.0f, 2.5f)

        val result = FilterUtils.removeOffsets(input, offsets)

        assertArrayEquals(expected, result, 0.01f)
    }
}
