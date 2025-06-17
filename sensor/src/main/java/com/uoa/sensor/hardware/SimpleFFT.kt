package com.uoa.sensor.hardware

import kotlin.math.*

/**
 * A simple radix-2 Cooley-Tukey FFT implementation for real-valued inputs.
 * Suitable for production use in Android environments without external libraries.
 */
object SimpleFFT {

    /**
     * Performs an in-place FFT on the given real and imaginary arrays.
     * Both arrays must have the same size, which must be a power of 2.
     *
     * @param real Real part of input/output.
     * @param imag Imaginary part of input/output.
     * @throws IllegalArgumentException if size is not a power of 2.
     */
    fun fft(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        if (n == 0 || (n and (n - 1)) != 0) {
            throw IllegalArgumentException("FFT size must be a power of 2")
        }

        val levels = 31 - Integer.numberOfLeadingZeros(n)
        val cosTable = DoubleArray(n / 2) { cos(2 * PI * it / n) }
        val sinTable = DoubleArray(n / 2) { sin(2 * PI * it / n) }

        // Bit-reversed addressing permutation
        for (i in 0 until n) {
            val j = Integer.reverse(i).ushr(32 - levels)
            if (j > i) {
                real[i] = real[j].also { real[j] = real[i] }
                imag[i] = imag[j].also { imag[j] = imag[i] }
            }
        }

        var size = 2
        while (size <= n) {
            val halfSize = size / 2
            val tableStep = n / size
            for (i in 0 until n step size) {
                for (j in 0 until halfSize) {
                    val k = j * tableStep
                    val tpre = real[i + j + halfSize] * cosTable[k] + imag[i + j + halfSize] * sinTable[k]
                    val tpim = -real[i + j + halfSize] * sinTable[k] + imag[i + j + halfSize] * cosTable[k]

                    real[i + j + halfSize] = real[i + j] - tpre
                    imag[i + j + halfSize] = imag[i + j] - tpim
                    real[i + j] += tpre
                    imag[i + j] += tpim
                }
            }
            size *= 2
        }
    }
}
