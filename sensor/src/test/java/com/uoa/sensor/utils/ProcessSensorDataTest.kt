package com.uoa.sensor.utils


import android.hardware.Sensor
import org.junit.Assert.*
import org.junit.Test


class ProcessSensorDataTest {

    @Test
    fun testProcessAccelerometerData() {
        val sensorType = Sensor.TYPE_ACCELEROMETER
        val values = floatArrayOf(1.0f, 2.0f, 3.0f)
        val expected = floatArrayOf(0f, 0f, 0f) // Adjust this based on expected output

        val result = ProcessSensorData.processSensorData(sensorType, values)

        assertArrayEquals(expected, result, 0.01f)
    }

    @Test
    fun testProcessLinearAccelerationData() {
        val sensorType = Sensor.TYPE_LINEAR_ACCELERATION
        val values = floatArrayOf(1.0f, 2.0f, 3.0f)
        val expected = floatArrayOf(0.5f, 1.0f, 1.5f) // Adjust this based on expected output

        val result = ProcessSensorData.processSensorData(sensorType, values)

        assertArrayEquals(expected, result, 0.01f)
    }

    @Test
    fun testProcessGyroscopeData() {
        val sensorType = Sensor.TYPE_GYROSCOPE
        val values = floatArrayOf(1.0f, 2.0f, 3.0f)
        val expected = floatArrayOf(0.5f, 1.0f, 1.5f) // Adjust this based on expected output

        val result = ProcessSensorData.processSensorData(sensorType, values)

        assertArrayEquals(expected, result, 0.01f)
    }

    @Test
    fun testProcessGravityData() {
        val sensorType = Sensor.TYPE_GRAVITY
        val values = floatArrayOf(1.0f, 2.0f, 3.0f)
        val expected = floatArrayOf(0.5f, 1.0f, 1.5f) // Adjust this based on expected output

        val result = ProcessSensorData.processSensorData(sensorType, values)

        assertArrayEquals(expected, result, 0.01f)
    }

    @Test
    fun testProcessDefaultData() {
        val sensorType = -1 // Invalid sensor type to trigger default case
        val values = floatArrayOf(1.0f, 2.0f, 3.0f)
        val expected = floatArrayOf(1.0f, 2.0f, 3.0f)

        val result = ProcessSensorData.processSensorData(sensorType, values)

        assertArrayEquals(expected, result, 0.01f)
    }

    @Test
    fun testCheckForNaN() {
        val values = listOf(1.0f, Float.NaN, 3.0f)
        val expected = listOf(1.0f, 0.0f, 3.0f)

        val result = ProcessSensorData.checkForNaN(values)

        assertEquals(expected, result)
    }
}
