package com.uoa.sensor.utils

import android.hardware.Sensor
import android.util.Log
import com.uoa.sensor.utils.FilterUtils.removeOffsets
import com.uoa.sensor.utils.ProcessSensorData.applyRotationMatrixToValues

object ProcessSensorData {

 fun processSensorData(sensorType: Int, values: FloatArray, rotationMatrix: FloatArray): FloatArray {
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            return checkForNaN(values.toList()).toFloatArray()
        }
        // Only apply rotation matrix to the relevant sensors
        val transformedValues = when (sensorType) {
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_MAGNETIC_FIELD -> {
                applyRotationMatrixToValues(values, rotationMatrix)
            }else -> values // No transformation needed for other sensors
        }

        // Process the (potentially transformed) sensor values
        return when (sensorType) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                // Apply offset removal if calibration data is available (assuming offsets are known)
                val offsets = floatArrayOf(0f, 0f, 0f) // Replace with actual calibration offsets if available
                val adjustedValues = FilterUtils.removeOffsets(transformedValues, offsets)
                checkForNaN(adjustedValues.toList()).toFloatArray()
            }
            Sensor.TYPE_GYROSCOPE, Sensor.TYPE_MAGNETIC_FIELD -> {
                checkForNaN(transformedValues.toList()).toFloatArray()
            }
            Sensor.TYPE_GRAVITY -> {
                checkForNaN(values.toList()).toFloatArray()
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                checkForNaN(values.toList()).toFloatArray()
            }
            else -> checkForNaN(values.toList()).toFloatArray()
        }
    }

    private fun applyRotationMatrixToValues(values: FloatArray, rotationMatrix: FloatArray): FloatArray {
        val transformedValues = FloatArray(3)
        transformedValues[0] = rotationMatrix[0] * values[0] + rotationMatrix[1] * values[1] + rotationMatrix[2] * values[2]
        transformedValues[1] = rotationMatrix[3] * values[0] + rotationMatrix[4] * values[1] + rotationMatrix[5] * values[2]
        transformedValues[2] = rotationMatrix[6] * values[0] + rotationMatrix[7] * values[1] + rotationMatrix[8] * values[2]
        return transformedValues
    }

    fun checkForNaN(values: List<Float>): List<Float> {
//        Log.d("HardwareModule", "Sensor data received : ${values.joinToString(", ")}")
        return values.map { if (it.isNaN()) 0f else it }
    }

    fun logSensorValues(sensorTypeName: String, values: List<Float>, stage: String) {
        Log.d("HardwareModule", "Sensor data $stage received: $sensorTypeName : ${values.joinToString(", ")}")
    }

}
