//package com.uoa.sensor.utils
//
//import android.hardware.Sensor
//
//object SensorProcessor {
//
//    private var gravity = FloatArray(3)
//
//    fun processSensorData(sensorType: Int, values: FloatArray, offsets: FloatArray = floatArrayOf(0f, 0f, 0f), alpha: Float = 0.8f): FloatArray {
//        return when (sensorType) {
//            Sensor.TYPE_ACCELEROMETER -> {
//                // Isolate gravity using low-pass filter
//                FilterUtils.lowPassFilter(values, gravity, alpha)
//
//                // Subtract gravity from accelerometer values to get linear acceleration
//                val linearAcceleration = FloatArray(3)
//                for (i in 0..2) {
//                    linearAcceleration[i] = values[i] - gravity[i]
//                }
//
//                // High-pass filter to further remove gravity influence
//                FilterUtils.highPassFilter(linearAcceleration, linearAcceleration, alpha)
//
//                checkForNaN(linearAcceleration.toList()).toFloatArray() // Ensure no NaNs
//            }
//            Sensor.TYPE_LINEAR_ACCELERATION -> {
//                // Apply offset removal (using provided or default offsets)
//                val adjustedValues = FilterUtils.removeOffsets(values, offsets)
//
//                // Denoise with moving average filter
//                DenoiseUtils.movingAverageFilter(adjustedValues.toList(), 2).toFloatArray()
//            }
//            Sensor.TYPE_GYROSCOPE, Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_GRAVITY -> {
//                // Directly return denoised values (these sensors are typically pre-filtered)
//                DenoiseUtils.movingAverageFilter(values.toList(), 2).toFloatArray()
//            }
//            else -> checkForNaN(values.toList()).toFloatArray()
//        }
//    }
//
//
//    private fun checkForNaN(values: List<Float>): List<Float> {
//        return values.map { if (it.isNaN()) 0f else it }
//    }
//
//    // ... (logging function remains the same)
//}
//
