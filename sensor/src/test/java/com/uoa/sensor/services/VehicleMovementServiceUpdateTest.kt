package com.uoa.sensor.services

import com.uoa.sensor.repository.SensorDataColStateRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleMovementServiceUpdateTest {

    private class TestVehicleMovementService : VehicleMovementServiceUpdate() {
        var autoStartCalled = false
        override suspend fun safeAutoStart() {
            autoStartCalled = true
        }
        override suspend fun safeAutoStop() { }
    }

    @Test
    fun `vehicle motion triggers auto start`() = runBlocking {
        val repo = SensorDataColStateRepository()
        val service = TestVehicleMovementService().apply {
            sensorRepo = repo
            movementStartDelay = 0L
        }
        repo.updateVehicleMovementStatus(true)
        service.handlePossibleStart()
        delay(50)
        assertTrue(service.autoStartCalled)
    }

    @Test
    fun `walking does not trigger auto start`() = runBlocking {
        val repo = SensorDataColStateRepository()
        val service = TestVehicleMovementService().apply {
            sensorRepo = repo
            movementStartDelay = 0L
        }
        repo.updateMovementType("walking")
        repo.updateVehicleMovementStatus(false)
        service.handlePossibleStart()
        delay(50)
        assertFalse(service.autoStartCalled)
    }

    @Test
    fun `running does not trigger auto start`() = runBlocking {
        val repo = SensorDataColStateRepository()
        val service = TestVehicleMovementService().apply {
            sensorRepo = repo
            movementStartDelay = 0L
        }
        repo.updateMovementType("running")
        repo.updateVehicleMovementStatus(false)
        service.handlePossibleStart()
        delay(50)
        assertFalse(service.autoStartCalled)
    }

    @Test
    fun `high speed alone triggers vehicle start`() = runBlocking {
        val repo = SensorDataColStateRepository()
        val service = TestVehicleMovementService().apply {
            sensorRepo = repo
            movementStartDelay = 0L
        }

        repo.updateSpeed(6.0)
        service.handlePossibleStart()

        delay(50)
        assertTrue(service.autoStartCalled)
    }

    @Test
    fun `walking label with low speed keeps vehicle false`() = runBlocking {
        val repo = SensorDataColStateRepository()

        repo.updateMovementType("walking")
        repo.updateSpeed(0.8)

        assertFalse(repo.isVehicleMoving.value)
    }
}
