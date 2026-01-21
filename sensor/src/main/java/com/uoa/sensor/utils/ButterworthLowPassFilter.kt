package com.uoa.sensor.utils

import kotlin.math.cos
import kotlin.math.sin

class ButterworthLowPassFilter(
    samplingRateHz: Double,
    cutoffHz: Double
) {
    private val section1 = createCoefficients(samplingRateHz, cutoffHz, Q_SECTION_1)
    private val section2 = createCoefficients(samplingRateHz, cutoffHz, Q_SECTION_2)
    private val axisFilters = Array(3) {
        AxisFilter(Biquad(section1), Biquad(section2))
    }

    fun filter(values: FloatArray): FloatArray {
        if (values.size < 3) return values
        val output = FloatArray(values.size)
        for (axis in 0 until 3) {
            output[axis] = axisFilters[axis].filter(values[axis].toDouble()).toFloat()
        }
        for (index in 3 until values.size) {
            output[index] = values[index]
        }
        return output
    }

    fun reset() {
        axisFilters.forEach { it.reset() }
    }

    private class AxisFilter(
        private val first: Biquad,
        private val second: Biquad
    ) {
        fun filter(x: Double): Double {
            return second.filter(first.filter(x))
        }

        fun reset() {
            first.reset()
            second.reset()
        }
    }

    private class Biquad(private val coeffs: BiquadCoefficients) {
        private var z1 = 0.0
        private var z2 = 0.0

        fun filter(x: Double): Double {
            val y = x * coeffs.b0 + z1
            z1 = x * coeffs.b1 + z2 - coeffs.a1 * y
            z2 = x * coeffs.b2 - coeffs.a2 * y
            return y
        }

        fun reset() {
            z1 = 0.0
            z2 = 0.0
        }
    }

    private data class BiquadCoefficients(
        val b0: Double,
        val b1: Double,
        val b2: Double,
        val a1: Double,
        val a2: Double
    )

    private fun createCoefficients(
        samplingRateHz: Double,
        cutoffHz: Double,
        q: Double
    ): BiquadCoefficients {
        val omega = 2.0 * Math.PI * cutoffHz / samplingRateHz
        val cosOmega = cos(omega)
        val alpha = sin(omega) / (2.0 * q)
        val b0 = (1.0 - cosOmega) / 2.0
        val b1 = 1.0 - cosOmega
        val b2 = (1.0 - cosOmega) / 2.0
        val a0 = 1.0 + alpha
        val a1 = -2.0 * cosOmega
        val a2 = 1.0 - alpha
        return BiquadCoefficients(
            b0 = b0 / a0,
            b1 = b1 / a0,
            b2 = b2 / a0,
            a1 = a1 / a0,
            a2 = a2 / a0
        )
    }

    companion object {
        private const val Q_SECTION_1 = 0.5411961
        private const val Q_SECTION_2 = 1.3065629
    }
}
