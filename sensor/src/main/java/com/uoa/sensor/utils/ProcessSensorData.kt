package com.uoa.sensor.utils

import android.hardware.Sensor
import android.util.Log

object ProcessSensorData {

    fun processSensorData(sensorType: Int, values: FloatArray): FloatArray {
        return when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> {
                val alpha = 0.8f
                val gravity = FloatArray(3)
                // Apply low-pass filter to isolate gravity
                FilterUtils.lowPassFilter(values, gravity, alpha)
                // gravity array is now updated with the filtered values

                // Remove gravity from accelerometer data to get linear acceleration
                val linearAcceleration = FloatArray(3)
                for (i in 0..2) {
                    linearAcceleration[i] = values[i] - gravity[i]
                }
                // Apply high-pass filter to remove any remaining gravity influence
                FilterUtils.highPassFilter(linearAcceleration, linearAcceleration, alpha)
                // Denoise using moving average filter
                val denoisedData = DenoiseUtils.movingAverageFilter(linearAcceleration.toList(), 2)
                // Normalize using z-score normalization
                val normalizedData = NormalizeUtils.zScoreNormalize(denoisedData)
                checkForNaN(normalizedData).toFloatArray()
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                // Apply offset removal if calibration data is available (assuming offsets are known)
                val offsets = floatArrayOf(0f, 0f, 0f) // Replace with actual calibration offsets if available
                val adjustedValues = FilterUtils.removeOffsets(values, offsets)
                // Apply median filter to reduce noise
                val denoisedData = DenoiseUtils.medianFilter(adjustedValues.toList(), 2)
                // Normalize using min-max normalization
                val normalizedData = NormalizeUtils.minMaxNormalize(denoisedData)
                checkForNaN(normalizedData).toFloatArray()
            }
            Sensor.TYPE_GYROSCOPE, Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_MAGNETIC_FIELD -> {
                // Apply moving average filter to reduce noise
                val denoisedData = DenoiseUtils.movingAverageFilter(values.toList(), 2)
                // Normalize using z-score normalization
                val normalizedData = NormalizeUtils.zScoreNormalize(denoisedData)
                checkForNaN(normalizedData).toFloatArray()
            }
            Sensor.TYPE_GRAVITY -> {
                // Gravity data is usually already low-pass filtered, but you can still apply additional filtering if needed
                val denoisedData = DenoiseUtils.movingAverageFilter(values.toList(), 2)
                // Normalize using min-max normalization
                val normalizedData = NormalizeUtils.minMaxNormalize(denoisedData)
                checkForNaN(normalizedData).toFloatArray()
            }
            else -> checkForNaN(values.toList()).toFloatArray()
        }
    }
    fun checkForNaN(values: List<Float>): List<Float> {
        return values.map { if (it.isNaN()) 0f else it }
    }

    fun logSensorValues(sensorTypeName: String, values: List<Float>, stage: String) {
        Log.d("HardwareModule", "Sensor data $stage received: $sensorTypeName : ${values.joinToString(", ")}")
    }
}