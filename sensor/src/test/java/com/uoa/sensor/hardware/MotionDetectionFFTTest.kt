package com.uoa.sensor.hardware

import com.uoa.core.database.entities.FFTFeatureDao
import com.uoa.sensor.repository.SensorDataColStateRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Verifies that leaving the vehicle state resets the vehicle movement flag.
 */
class MotionDetectionFFTTest {

    @Test
    fun `isVehicleMoving resets when switching from vehicle to other modes`() = runBlocking {
        val sig = mock(SignificantMotionSensor::class.java)
        val lin = mock(LinearAccelerationSensor::class.java)
        val acc = mock(AccelerometerSensor::class.java)
        val dao = mock(FFTFeatureDao::class.java)
        val repo = SensorDataColStateRepository()

        val detector = MotionDetectionFFT(sig, lin, acc, dao, repo)

        val toVehicle = MotionDetectionFFT::class.java.getDeclaredMethod("transitionToVehicle", Long::class.javaPrimitiveType).apply { isAccessible = true }
        val toWalking = MotionDetectionFFT::class.java.getDeclaredMethod("transitionToWalking", Long::class.javaPrimitiveType).apply { isAccessible = true }
        val toRunning = MotionDetectionFFT::class.java.getDeclaredMethod("transitionToRunning", Long::class.javaPrimitiveType).apply { isAccessible = true }
        val toStationary = MotionDetectionFFT::class.java.getDeclaredMethod("transitionToStationary", Long::class.javaPrimitiveType).apply { isAccessible = true }

        toVehicle.invoke(detector, System.currentTimeMillis())
        delay(100)
        assertTrue(repo.isVehicleMoving.value)

        toWalking.invoke(detector, System.currentTimeMillis())
        delay(100)
        assertFalse(repo.isVehicleMoving.value)

        toVehicle.invoke(detector, System.currentTimeMillis())
        delay(100)
        toRunning.invoke(detector, System.currentTimeMillis())
        delay(100)
        assertFalse(repo.isVehicleMoving.value)

        toVehicle.invoke(detector, System.currentTimeMillis())
        delay(100)
        toStationary.invoke(detector, System.currentTimeMillis())
        delay(100)
        assertFalse(repo.isVehicleMoving.value)
    }
}

