package com.uoa.sensor.data


import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.uoa.core.database.daos.UnsafeBehaviourDao
import com.uoa.core.database.entities.UnsafeBehaviourEntity
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.utils.toEntity
import com.uoa.sensor.repository.UnsafeBehaviourRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*

class UnsafeBehaviourRepositoryTest {

    private lateinit var unsafeBehaviourDao: UnsafeBehaviourDao
    private lateinit var repository: UnsafeBehaviourRepository

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        unsafeBehaviourDao = mock()
        repository = UnsafeBehaviourRepository(unsafeBehaviourDao)
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInsertUnsafeBehaviour() = runTest {
        val unsafeBehaviour = UnsafeBehaviourModel(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Speeding", 5.0f, System.currentTimeMillis(), Date(), false, "Over speed limit")
        repository.insertUnsafeBehaviour(unsafeBehaviour)
        verify(unsafeBehaviourDao).insertUnsafeBehaviour(unsafeBehaviour.toEntity())
    }

    @Test
    fun testInsertUnsafeBehaviourBatch() = runTest {
        val unsafeBehaviours = listOf(
            UnsafeBehaviourModel(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Speeding", 5.0f, System.currentTimeMillis(), Date(), false, "Over speed limit"),
            UnsafeBehaviourModel(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Harsh Braking", 3.0f, System.currentTimeMillis(), Date(), false, "Sudden brake")
        )
        repository.insertUnsafeBehaviourBatch(unsafeBehaviours)
        verify(unsafeBehaviourDao).insertUnsafeBehaviourBatch(unsafeBehaviours.map { it.toEntity() })
    }

    @Test
    fun testUpdateUnsafeBehaviour() = runBlocking {
        val unsafeBehaviour = UnsafeBehaviourModel(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Speeding", 5.0f, System.currentTimeMillis(), Date(), false, "Over speed limit")
        repository.updateUnsafeBehaviour(unsafeBehaviour)
        verify(unsafeBehaviourDao).updateUnsafeBehaviour(unsafeBehaviour.toEntity())
    }

    @Test
    fun testDeleteUnsafeBehaviour() = runTest {
        val unsafeBehaviour = UnsafeBehaviourModel(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Speeding", 5.0f, System.currentTimeMillis(), Date(), false, "Over speed limit")
        repository.deleteUnsafeBehaviour(unsafeBehaviour)
        verify(unsafeBehaviourDao).deleteUnsafeBehaviour(unsafeBehaviour.toEntity())
    }

    @Test
    fun testGetUnsafeBehavioursByTripId() = runTest {
        val tripId = UUID.randomUUID()
        val unsafeBehaviourEntities = listOf(
            UnsafeBehaviourEntity(UUID.randomUUID(), tripId, UUID.randomUUID(), "Speeding", 5.0f, System.currentTimeMillis(), Date(), false, "Over speed limit"),
            UnsafeBehaviourEntity(UUID.randomUUID(), tripId, UUID.randomUUID(), "Harsh Braking", 3.0f, System.currentTimeMillis(), Date(), false, "Sudden brake")
        )
        whenever(unsafeBehaviourDao.getUnsafeBehavioursByTripId(tripId)).thenReturn(flowOf(unsafeBehaviourEntities))

        val result: Flow<List<UnsafeBehaviourModel>> = repository.getUnsafeBehavioursByTripId(tripId)
        result.collect { unsafeBehaviours ->
            assertEquals(unsafeBehaviourEntities.size, unsafeBehaviours.size)
        }
        verify(unsafeBehaviourDao).getUnsafeBehavioursByTripId(tripId)
    }

    @Test
    fun testGetUnsafeBehaviourById() = runTest {
        val id = UUID.randomUUID()
        val unsafeBehaviourEntity = UnsafeBehaviourEntity(id, UUID.randomUUID(), UUID.randomUUID(), "Speeding", 5.0f, System.currentTimeMillis(), Date(), false, "Over speed limit")
        whenever(unsafeBehaviourDao.getUnsafeBehaviourById(id)).thenReturn(unsafeBehaviourEntity)

        val result = repository.getUnsafeBehaviourById(id)
        assertEquals(id, result?.id)
        verify(unsafeBehaviourDao).getUnsafeBehaviourById(id)
    }

    @Test
    fun testGetUnsafeBehavioursBySyncStatus() = runTest {
        val syncStatus = false
        val unsafeBehaviourEntities = listOf(
            UnsafeBehaviourEntity(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Speeding", 5.0f, System.currentTimeMillis(), Date(), syncStatus, "Over speed limit")
        )
        whenever(unsafeBehaviourDao.getUnsafeBehavioursBySyncStatus(syncStatus)).thenReturn(unsafeBehaviourEntities)

        val result = repository.getUnsafeBehavioursBySyncStatus(syncStatus)
        assertEquals(unsafeBehaviourEntities.size, result.size)
        verify(unsafeBehaviourDao).getUnsafeBehavioursBySyncStatus(syncStatus)
    }

    @Test
    fun testDeleteAllUnsafeBehavioursBySyncStatus() = runBlocking {
        val syncStatus = false
        repository.deleteAllUnsafeBehavioursBySyncStatus(syncStatus)
        verify(unsafeBehaviourDao).deleteAllUnsafeBehavioursBySyncStatus(syncStatus)
    }

    @Test
    fun testDeleteAllUnsafeBehaviours() = runBlocking {
        repository.deleteAllUnsafeBehaviours()
        verify(unsafeBehaviourDao).deleteAllUnsafeBehaviours()
    }

    @Test
    fun testGetUnsafeBehaviourCountByTypeAndTime() = runTest {
        val behaviorType = "Speeding"
        val startTime = System.currentTimeMillis() - 100000
        val endTime = System.currentTimeMillis()
        val count = 5
        whenever(unsafeBehaviourDao.getUnsafeBehaviourCountByTypeAndTime(behaviorType, startTime, endTime)).thenReturn(count)

        val result = repository.getUnsafeBehaviourCountByTypeAndTime(behaviorType, startTime, endTime)
        assertEquals(count, result)
        verify(unsafeBehaviourDao).getUnsafeBehaviourCountByTypeAndTime(behaviorType, startTime, endTime)
    }

    @Test
    fun testGetUnsafeBehaviourCountByTypeAndDistance() = runTest {
        val behaviorType = "Speeding"
        val tripId = UUID.randomUUID()
        val totalDistance = 1000f
        val count = 3
        whenever(unsafeBehaviourDao.getUnsafeBehaviourCountByTypeAndDistance(behaviorType, tripId, totalDistance)).thenReturn(count)

        val result = repository.getUnsafeBehaviourCountByTypeAndDistance(behaviorType, tripId, totalDistance)
        assertEquals(count, result)
        verify(unsafeBehaviourDao).getUnsafeBehaviourCountByTypeAndDistance(behaviorType, tripId, totalDistance)
    }

    @Test
    fun testGetUnsafeBehavioursBetweenDates() = runTest {
        val startDate = Date(System.currentTimeMillis() - 100000)
        val endDate = Date()
        val unsafeBehaviourEntities = listOf(
            UnsafeBehaviourEntity(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Speeding", 5.0f, System.currentTimeMillis(), Date(), false, "Over speed limit"),
            UnsafeBehaviourEntity(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Harsh Braking", 3.0f, System.currentTimeMillis(), Date(), false, "Sudden brake")
        )
        whenever(unsafeBehaviourDao.getUnsafeBehavioursBetweenDates(startDate, endDate)).thenReturn(flowOf(unsafeBehaviourEntities))

        val result: Flow<List<UnsafeBehaviourModel>> = repository.getUnsafeBehavioursBetweenDates(startDate, endDate)
        result.collect { unsafeBehaviours ->
            assertEquals(unsafeBehaviourEntities.size, unsafeBehaviours.size)
        }
        verify(unsafeBehaviourDao).getUnsafeBehavioursBetweenDates(startDate, endDate)
    }
}
