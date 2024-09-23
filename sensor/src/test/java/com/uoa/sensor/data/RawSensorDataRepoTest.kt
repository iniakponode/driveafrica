package com.uoa.sensor.data

import com.nhaarman.mockitokotlin2.mock
import com.uoa.core.database.daos.RawSensorDataDao
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import com.uoa.sensor.repository.RawSensorDataRepositoryImpl

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mockito.*
import java.time.Instant
import java.time.ZoneId
import java.util.*

@ExperimentalCoroutinesApi
class RawSensorDataRepoTest {

    private lateinit var rawSensorDataDao: RawSensorDataDao
    private lateinit var rawSensorDataRepositoryImpl: RawSensorDataRepositoryImpl

    @Before
    fun setUp() {
        rawSensorDataDao = mock()
        rawSensorDataRepositoryImpl = RawSensorDataRepositoryImpl(rawSensorDataDao)
    }

    @Test
    fun testGetRawSensorDataBetween() = runTest {
        val start = Instant.now()
        val end = Instant.now().plusSeconds(3600)
        val rawSensorDataEntity = RawSensorDataEntity(
            id = UUID.randomUUID(),
            // initialize other fields
            sensorType = 1,
            sensorTypeName = "Accelerometer",
            values = listOf(1.0f, 2.0f, 3.0f),
            timestamp = 1234567890,
            date = Date(),
            accuracy = 1,
            locationId = UUID.randomUUID(),
            tripId = UUID.randomUUID(),
            sync = false

        )

        `when`(rawSensorDataDao.getRawSensorDataBetween(start, end))
            .thenReturn(flowOf(listOf(rawSensorDataEntity)))

        val result = rawSensorDataRepositoryImpl.getRawSensorDataBetween(start, end).toList()

        assertEquals(1, result[0].size)
        assertEquals(rawSensorDataEntity.toDomainModel(), result[0][0])
    }

    @Test
    fun testGetRawSensorDataByTripId() = runTest {
        val tripId = UUID.randomUUID()
        val rawSensorDataEntity = RawSensorDataEntity(
            id = UUID.randomUUID(),
            // initialize other fields
            sensorType = 1,
            sensorTypeName = "Accelerometer",
            values = listOf(1.0f, 2.0f, 3.0f),
            timestamp = 1234567890,
            date = Date(),
            accuracy = 1,
            locationId = UUID.randomUUID(),
            tripId = tripId,
            sync = false


        )

        `when`(rawSensorDataDao.getRawSensorDataByTripId(tripId))
            .thenReturn(flowOf(listOf(rawSensorDataEntity)))

        val result = rawSensorDataRepositoryImpl.getRawSensorDataByTripId(tripId).toList()

        assertEquals(1, result[0].size)
        assertEquals(rawSensorDataEntity, result[0][0])
    }

    @Test
    fun testGetRawSensorDataByLocationId() = runTest {
        val rawSensorDataEntity = RawSensorDataEntity(
            id = UUID.randomUUID(),
            // initialize other fields
                sensorType = 1,
                sensorTypeName = "Accelerometer",
                values = listOf(1.0f, 2.0f, 3.0f),
                timestamp = 1234567890,
                date = Date(),
                accuracy = 1,
                locationId = UUID.randomUUID(),
                tripId = UUID.randomUUID(),
                sync = false
        )

        `when`(rawSensorDataDao.getRawSensorDataByLocationId(rawSensorDataEntity.locationId!!))
            .thenReturn(flowOf(listOf(rawSensorDataEntity)))

        val result = rawSensorDataRepositoryImpl.getRawSensorDataByLocationId(rawSensorDataEntity.locationId!!).toList()

        assertEquals(1, result[0].size)
        assertEquals(rawSensorDataEntity, result[0][0])
    }

    @Test
    fun testInsertRawSensorData() = runTest {
        val rawSensorData = RawSensorData(
            id = UUID.randomUUID(),
            // initialize other fields
            sensorType = 1,
            sensorTypeName = "Accelerometer",
            values = listOf(1.0f, 2.0f, 3.0f),
            timestamp = 1234567890,
            date = Date(),
            accuracy = 1,
            locationId = UUID.randomUUID(),
            tripId = UUID.randomUUID(),
            sync = false
        )

        rawSensorDataRepositoryImpl.insertRawSensorData(rawSensorData)

        verify(rawSensorDataDao).insertRawSensorData(rawSensorData.toEntity())
    }

    @Test
    fun testInsertRawSensorDataBatch() = runTest {
        val rawSensorDataList = listOf(
            RawSensorDataEntity(
                id = UUID.randomUUID(),
                // initialize other fields
                sensorType = 1,
                sensorTypeName = "Accelerometer",
                values = listOf(1.0f, 2.0f, 3.0f),
                timestamp = 1234567890,
                date = Date(),
                accuracy = 1,
                locationId = UUID.randomUUID(),
                tripId = UUID.randomUUID(),
                sync = false
            )
        )

        rawSensorDataRepositoryImpl.insertRawSensorDataBatch(rawSensorDataList)

        verify(rawSensorDataDao).insertRawSensorDataBatch(rawSensorDataList)
    }

    @Test
    fun testGetRawSensorDataById() = runTest {
        val id = UUID.randomUUID()
        val rawSensorDataEntity = RawSensorDataEntity(
            id = id,
            // initialize other fields
            sensorType = 1,
            sensorTypeName = "Accelerometer",
            values = listOf(1.0f, 2.0f, 3.0f),
            timestamp = 1234567890,
            date = Date(),
            accuracy = 1,
            locationId = UUID.randomUUID(),
            tripId = UUID.randomUUID(),
            sync = false
        )

        `when`(rawSensorDataDao.getRawSensorDataById(id)).thenReturn(rawSensorDataEntity)

        val result = rawSensorDataRepositoryImpl.getRawSensorDataById(id)

        assertEquals(rawSensorDataEntity, result)
    }

    @Test
    fun testGetUnsyncedRawSensorData() = runTest {
        val rawSensorDataEntity = RawSensorDataEntity(
            id = UUID.randomUUID(),
            // initialize other fields
            sensorType = 1,
            sensorTypeName = "Accelerometer",
            values = listOf(1.0f, 2.0f, 3.0f),
            timestamp = 1234567890,
            date = Date(),
            accuracy = 1,
            locationId = UUID.randomUUID(),
            tripId = UUID.randomUUID(),
            sync = false
        )

        `when`(rawSensorDataDao.getUnsyncedRawSensorData())
            .thenReturn(flowOf(listOf(rawSensorDataEntity)))

        val result = rawSensorDataRepositoryImpl.getUnsyncedRawSensorData().toList()

        assertEquals(1, result[0].size)
        assertEquals(rawSensorDataEntity, result[0][0])
    }

    @Test
    fun testUpdateRawSensorData() = runTest {
        val rawSensorDataEntity = RawSensorDataEntity(
            id = UUID.randomUUID(),
            // initialize other fields
            sensorType = 1,
            sensorTypeName = "Accelerometer",
            values = listOf(1.0f, 2.0f, 3.0f),
            timestamp = 1234567890,
            date = Date(),
            accuracy = 1,
            locationId = UUID.randomUUID(),
            tripId = UUID.randomUUID(),
            sync = false
        )

        rawSensorDataRepositoryImpl.updateRawSensorData(rawSensorDataEntity)

        verify(rawSensorDataDao).updateRawSensorData(rawSensorDataEntity)
    }

    @Test
    fun testDeleteAllRawSensorData() = runTest {
        rawSensorDataRepositoryImpl.deleteAllRawSensorData()

        verify(rawSensorDataDao).deleteAllRawSensorData()
    }

    @Test
    fun testGetSensorDataBetweenDates() = runTest {
        val startDate = Date()
        val endDate = Date()
        // Convert Long (epoch milliseconds) to LocalDate
        val zoneId = ZoneId.systemDefault()
        val sDate = Instant.ofEpochMilli(startDate.toInstant().toEpochMilli()).atZone(zoneId).toLocalDate()
        val eDate = Instant.ofEpochMilli(endDate.toInstant().toEpochMilli()).atZone(zoneId).toLocalDate()
        val rawSensorDataEntity = RawSensorDataEntity(
            id = UUID.randomUUID(),
            // initialize other fields
            sensorType = 1,
            sensorTypeName = "Accelerometer",
            values = listOf(1.0f, 2.0f, 3.0f),
            timestamp = 1234567890,
            date = Date(),
            accuracy = 1,
            locationId = UUID.randomUUID(),
            tripId = UUID.randomUUID(),
            sync = false
        )

        `when`(rawSensorDataDao.getSensorDataBetweenDates(sDate, eDate))
            .thenReturn(flowOf(listOf(rawSensorDataEntity)))

        val result = rawSensorDataRepositoryImpl.getSensorDataBetweenDates(sDate, eDate).toList()

        assertEquals(1, result[0].size)
        assertEquals(rawSensorDataEntity, result[0][0])
    }

    @Test
    fun testGetSensorDataByTripId() = runTest {
        val tripId = UUID.randomUUID()
        val rawSensorDataEntity = RawSensorDataEntity(
            id = UUID.randomUUID(),
            // initialize other fields
            sensorType = 1,
            sensorTypeName = "Accelerometer",
            values = listOf(1.0f, 2.0f, 3.0f),
            timestamp = 1234567890,
            date = Date(),
            accuracy = 1,
            locationId = UUID.randomUUID(),
            tripId = tripId,
            sync = false
        )

        `when`(rawSensorDataDao.getSensorDataByTripId(tripId))
            .thenReturn(flowOf(listOf(rawSensorDataEntity)))

        val result = rawSensorDataRepositoryImpl.getSensorDataByTripId(tripId).toList()

        assertEquals(1, result[0].size)
        assertEquals(rawSensorDataEntity, result[0][0])
    }
}