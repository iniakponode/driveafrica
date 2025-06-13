package com.uoa.sensor.data

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.uoa.core.database.daos.TripDao
import com.uoa.core.database.entities.TripEntity
import com.uoa.core.model.Trip
import com.uoa.core.utils.toEntity
import com.uoa.sensor.repository.TripDataRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

class TripDataRepositoryImplTest {

    private lateinit var tripDao: TripDao
    private lateinit var repository: TripDataRepositoryImpl

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        tripDao = mock()
        repository = TripDataRepositoryImpl(tripDao)
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInsertTrip() = runTest {
        val trip = Trip(UUID.randomUUID(), UUID.randomUUID(), Date().toInstant().toEpochMilli(), Date().toInstant().toEpochMilli(), Date(), Date(), "no alcohol", false)
        repository.insertTrip(trip)
        verify(tripDao).insertTrip(trip.toEntity())
    }

    @Test
    fun testUpdateTrip() = runBlocking {
        val trip = Trip(UUID.randomUUID(), UUID.randomUUID(), Date().toInstant().toEpochMilli(), Date().toInstant().toEpochMilli()-6000, Date(), Date(), "no alcohol", false)
        repository.updateTrip(trip)
        verify(tripDao).updateTrip(trip.toEntity())
    }

    @Test
    fun testGetAllTrips() = runTest {
        val tripEntities = listOf(
            TripEntity(UUID.randomUUID(), UUID.randomUUID(), Date(), Date(), Date().toInstant().toEpochMilli()-6000, Date().toInstant().toEpochMilli(), "no alcohol", false),
            TripEntity(UUID.randomUUID(), UUID.randomUUID(), Date(), Date(), Date().toInstant().toEpochMilli()-7000, Date().toInstant().toEpochMilli(), "no alcohol", false)
        )
        whenever(tripDao.getAllTrips()).thenReturn(tripEntities)
        val result = repository.getAllTrips()
        assert(result.size == tripEntities.size)
        verify(tripDao).getAllTrips()
    }

    @Test
    fun testGetTripById() = runTest {
        val id = UUID.randomUUID()
        val tripEntity = TripEntity(id, UUID.randomUUID(), Date(), Date(), 0L, 0L, "no alcohol", false)
        whenever(tripDao.getTripById(id)).thenReturn(tripEntity)
        val result = repository.getTripById(id)
        assert(result?.id == id)
        verify(tripDao).getTripById(id)
    }

    @Test
    fun testUpdateUploadStatus() = runBlocking {
        val id = UUID.randomUUID()
        val sync = true
        repository.updateUploadStatus(id, sync)
        verify(tripDao).updateUploadStatus(id, sync)
    }

    @Test
    fun testGetTripsByDriverProfileId() = runTest {
        val driverProfileId = UUID.randomUUID()
        val tripEntities = listOf(
            TripEntity(UUID.randomUUID(), driverProfileId, Date(), Date(), 0L, 0L, "no alcohol", false)
        )
        whenever(tripDao.getTripsByDriverProfileId(driverProfileId)).thenReturn(tripEntities)
        val result = repository.getTripsByDriverProfileId(driverProfileId)
        assert(result.size == tripEntities.size)
        verify(tripDao).getTripsByDriverProfileId(driverProfileId)
    }

    @Test
    fun testGetTripsBySyncStatus() = runTest {
        val syncStatus = false
        val tripEntities = listOf(
            TripEntity(UUID.randomUUID(), UUID.randomUUID(), Date(), Date(), 0L, 0L, "no alcohol",syncStatus)
        )
        whenever(tripDao.getTripsBySyncStatus(syncStatus)).thenReturn(tripEntities)
        val result = repository.getTripsBySyncStatus(syncStatus)
        assert(result.size == tripEntities.size)
        verify(tripDao).getTripsBySyncStatus(syncStatus)
    }

    @Test
    fun testDeleteTripById() = runBlocking {
        val id = UUID.randomUUID()
        repository.deleteTripById(id)
        verify(tripDao).deleteTripById(id)
    }
}
