// MotionFFTClassifier.kt
package com.uoa.sensor.hardware

import android.util.Log
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

class MotionFFTClassifier(
    private val sampleRate: Int = 10,   // ~10 Hz for SENSOR_DELAY_NORMAL
    private val bufferSize: Int = 64     // smaller window = faster reaction
) {
    private val buffer = FloatArray(bufferSize)
    private var index = 0
    private var isBufferFull = false

    data class ClassificationResult(
        val label: String,
        val energy: Double,
        val dominantFrequency: Double,
        val entropy: Double
    )

    companion object {
        private const val TAG = "MotionFFTClassifier"
        // ———– TUNABLE THRESHOLDS ———–
        private const val STATIONARY_ENERGY = 200.0
        private val WALKING_FREQ_RANGE = 0.8..4.5
        private val WALKING_ENERGY_RANGE = 200.0..4_000.0
        private val RUNNING_FREQ_RANGE = 4.5..8.0
        private val RUNNING_ENERGY_RANGE = 4_000.0..12_000.0
        private const val VEHICLE_ENERGY = 4_000.0
        private const val LOW_FREQ_VEHICLE_MAX = 0.8
    }

    /**
     * Add a new magnitude. Returns true *only* when buffer just filled.
     */
    fun addSample(magnitude: Double): Boolean {
        buffer[index++] = magnitude.toFloat()
        if (index >= bufferSize) {
            index = 0
            isBufferFull = true
            return true
        }
        return false
    }

    /**
     * Run the FFT on the buffer, classify, then reset the buffer‐full flag.
     */
    fun classify(): ClassificationResult {
        if (!isBufferFull) {
            throw IllegalStateException("Buffer not yet full")
        }

        // 1) Copy into real/imag arrays
        val real = DoubleArray(bufferSize) { buffer[it].toDouble() }
        val imag = DoubleArray(bufferSize) { 0.0 }

        // 2) Remove DC offset (mean) so we get nonzero freq components
        val mean = real.average()
        for (i in real.indices) real[i] -= mean

        // 3) FFT
        SimpleFFT.fft(real, imag)

        // 4) Compute half-spectrum magnitudes
        val mags = DoubleArray(bufferSize / 2) { i ->
            sqrt(real[i].pow(2) + imag[i].pow(2))
        }

        // 5) Energy & Dominant Freq
        val energy = mags.sumOf { it * it }
        val peakIdx = mags.indices.maxByOrNull { mags[it] } ?: 0
        val freq = peakIdx * sampleRate.toDouble() / bufferSize

        // 6) Entropy
        val totalMag = mags.sum().takeIf { it > 0 } ?: 1.0
        val entropy = mags
            .map { it / totalMag }
            .filter { it > 0 }
            .sumOf { -it * ln(it) }

        // 7) Rule-based labeling
        val label = when {
            energy < STATIONARY_ENERGY -> "stationary"
            freq in WALKING_FREQ_RANGE && energy in WALKING_ENERGY_RANGE -> "walking"
            freq in RUNNING_FREQ_RANGE && energy in RUNNING_ENERGY_RANGE -> "running"
            freq < LOW_FREQ_VEHICLE_MAX && energy >= STATIONARY_ENERGY -> "vehicle"
            energy >= VEHICLE_ENERGY -> "vehicle"
            else -> "unknown"
        }

        Log.d(
            TAG, "Classification: $label | Energy: ${"%.1f".format(energy)}" +
                    " | Freq: ${"%.2f".format(freq)} | Entropy: ${"%.2f".format(entropy)}"
        )

        // Reset for the next window
        isBufferFull = false

        return ClassificationResult(label, energy, freq, entropy)
    }
}
