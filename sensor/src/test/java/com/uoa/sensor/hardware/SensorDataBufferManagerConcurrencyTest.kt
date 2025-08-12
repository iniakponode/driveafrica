package com.uoa.sensor.hardware

import com.nhaarman.mockitokotlin2.*
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.model.RawSensorData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Date
import java.util.UUID

class SensorDataBufferManagerConcurrencyTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun concurrentAddAndFlushDoesNotCrash() = runTest {
        val repository: RawSensorDataRepository = mock()
        val bufferManager = SensorDataBufferManager(repository)

        val sampleData = RawSensorData(
            id = UUID.randomUUID(),
            sensorType = 1,
            sensorTypeName = "Test",
            values = listOf(0f, 0f, 0f),
            timestamp = 0L,
            date = Date(),
            accuracy = 0,
            driverProfileId = UUID.randomUUID()
        )

        val jobAdd = launch {
            repeat(100) {
                bufferManager.addToSensorBuffer(sampleData.copy(id = UUID.randomUUID()))
            }
        }

        val jobProcess = launch {
            repeat(10) {
                bufferManager.processAndStoreSensorData()
            }
        }

        jobAdd.join()
        jobProcess.join()
        bufferManager.flushBufferToDatabase()

        verify(repository, atLeastOnce()).processAndStoreSensorData(any())
    }
}
