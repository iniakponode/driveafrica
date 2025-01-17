package com.uoa.sensor.data


import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.daos.RawSensorDataDao
import com.uoa.core.database.entities.LocationEntity
import com.uoa.sensor.repository.LocationRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.util.*

class LocationRepositoryImplTest {

    private lateinit var locationDao: LocationDao
    private lateinit var rawsensorDao: RawSensorDataDao
    private lateinit var repository: LocationRepositoryImpl

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        locationDao = mock(LocationDao::class.java)
        rawsensorDao= mock(RawSensorDataDao::class.java)
        repository = LocationRepositoryImpl(locationDao, rawsensorDao )
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInsertLocation() = runTest {
        val location = LocationEntity(UUID.randomUUID(), 1.0, 2.0, System.currentTimeMillis(), Date(), 3.0, 4.0f, 5.0f, false, false)
        repository.insertLocation(location)
        verify(locationDao).insertLocation(location)
    }

    @Test
    fun testInsertLocationBatch() = runTest {
        val locations = listOf(
            LocationEntity(UUID.randomUUID(), 1.0, 2.0, System.currentTimeMillis(), Date(), 3.0, 4.0f, 5.0f,false, false),
            LocationEntity(UUID.randomUUID(), 6.0, 7.0, System.currentTimeMillis(), Date(), 8.0, 9.0f, 10.0f, true, true)
        )
        repository.insertLocationBatch(locations)
        verify(locationDao).insertLocationBatch(locations)
    }

    @Test
    fun testGetLocationById(): Unit = runTest {
        val id = UUID.randomUUID()
        val location = LocationEntity(id, 1.0, 2.0, System.currentTimeMillis(), Date(), 3.0, 4.0f, 5.0f, false, false)
        `when`(locationDao.getLocationById(id)).thenReturn(location)
        val result = repository.getLocationById(id)
        assert(result == location)
        verify(locationDao).getLocationById(id)
    }

    @Test
    fun testGetLocationBySynced() = runTest{
        val locations = listOf(
            LocationEntity(UUID.randomUUID(), 1.0, 2.0, System.currentTimeMillis(), Date(), 3.0, 4.0f, 5.0f, false, false),
            LocationEntity(UUID.randomUUID(), 6.0, 7.0, System.currentTimeMillis(), Date(), 8.0, 9.0f, 10.0f, true, true)
        )
        `when`(locationDao.getLocationBySyncStatus(false)).thenReturn(flowOf(locations))
        val result = repository.getLocationBySynced(false)
        assert(result.first() == locations)
        verify(locationDao).getLocationBySyncStatus(false)
    }

    @Test
    fun testUpdateLocation() = runBlocking {
        val location = LocationEntity(UUID.randomUUID(), 1.0, 2.0, System.currentTimeMillis(), Date(), 3.0, 4.0f, 5.0f, false, false)
        repository.updateLocation(location)
        verify(locationDao).updateLocation(location)
    }

    @Test
    fun testDeleteAllLocations() = runBlocking {
        repository.deleteAllLocations()
        verify(locationDao).deleteAllLocations()
    }
}
