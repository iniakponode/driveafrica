package com.uoa.core.daotest

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.uoa.core.Sdaddb
import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.entities.LocationEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LocationDaoTest {

    private lateinit var database: Sdaddb
    private lateinit var locationDao: LocationDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            Sdaddb::class.java
        ).allowMainThreadQueries().build()
        locationDao = database.locationDataDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testInsertAndGetLocation() = runTest {
        val location = LocationEntity(
            id = UUID.randomUUID(),
            latitude = 1.0,
            longitude = 1.0,
            timestamp = System.currentTimeMillis(),
            date = Date(),
            altitude = 1.0,
            speed = 1.0f,
            distance = 1.0f,
            sync = false
        )

        locationDao.insertLocation(location)
        val fetchedLocation = locationDao.getLocationById(location.id)
        assertNotNull(fetchedLocation)
        assertEquals(location.id, fetchedLocation?.id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testInsertLocationBatch() = runTest {
        val location1 = LocationEntity(
            id = UUID.randomUUID(),
            latitude = 1.0,
            longitude = 1.0,
            timestamp = System.currentTimeMillis(),
            date = Date(),
            altitude = 1.0,
            speed = 1.0f,
            distance = 1.0f,
            sync = false
        )

        val location2 = LocationEntity(
            id = UUID.randomUUID(),
            latitude = 2.0,
            longitude = 2.0,
            timestamp = System.currentTimeMillis(),
            date = Date(),
            altitude = 2.0,
            speed = 2.0f,
            distance = 2.0f,
            sync = false
        )

        locationDao.insertLocationBatch(listOf(location1, location2))
        val allLocations = locationDao.getAllLocations()
        assertEquals(2, allLocations.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testGetAllLocations() = runTest {
        val location1 = LocationEntity(
            id = UUID.randomUUID(),
            latitude = 1.0,
            longitude = 1.0,
            timestamp = System.currentTimeMillis(),
            date = Date(),
            altitude = 1.0,
            speed = 1.0f,
            distance = 1.0f,
            sync = false
        )

        val location2 = LocationEntity(
            id = UUID.randomUUID(),
            latitude = 2.0,
            longitude = 2.0,
            timestamp = System.currentTimeMillis(),
            date = Date(),
            altitude = 2.0,
            speed = 2.0f,
            distance = 2.0f,
            sync = false
        )

        locationDao.insertLocationBatch(listOf(location1, location2))
        val allLocations = locationDao.getAllLocations()
        assertEquals(2, allLocations.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testGetLocationById() = runTest {
        val location = LocationEntity(
            id = UUID.randomUUID(),
            latitude = 1.0,
            longitude = 1.0,
            timestamp = System.currentTimeMillis(),
            date = Date(),
            altitude = 1.0,
            speed = 1.0f,
            distance = 1.0f,
            sync = false
        )

        locationDao.insertLocation(location)
        val fetchedLocation = locationDao.getLocationById(location.id)
        assertNotNull(fetchedLocation)
        assertEquals(location.id, fetchedLocation?.id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testGetLocationBySyncStatus() = runTest {
        val location1 = LocationEntity(
            id = UUID.randomUUID(),
            latitude = 1.0,
            longitude = 1.0,
            timestamp = System.currentTimeMillis(),
            date = Date(),
            altitude = 1.0,
            speed = 1.0f,
            distance = 1.0f,
            sync = false
        )

        val location2 = LocationEntity(
            id = UUID.randomUUID(),
            latitude = 2.0,
            longitude = 2.0,
            timestamp = System.currentTimeMillis(),
            date = Date(),
            altitude = 2.0,
            speed = 2.0f,
            distance = 2.0f,
            sync = true
        )

        locationDao.insertLocationBatch(listOf(location1, location2))

        val unsyncedLocations = locationDao.getLocationBySyncStatus(false).first()
        val syncedLocations = locationDao.getLocationBySyncStatus(true).first()

        assertEquals(1, unsyncedLocations.size)
        assertEquals(location1.id, unsyncedLocations[0].id)

        assertEquals(1, syncedLocations.size)
        assertEquals(location2.id, syncedLocations[0].id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testUpdateLocation() = runTest {
        val location = LocationEntity(
            id = UUID.randomUUID(),
            latitude = 1.0,
            longitude = 1.0,
            timestamp = System.currentTimeMillis(),
            date = Date(),
            altitude = 1.0,
            speed = 1.0f,
            distance = 1.0f,
            sync = false
        )

        locationDao.insertLocation(location)
        val updatedLocation = location.copy(sync = true)
        locationDao.updateLocation(updatedLocation)

        val fetchedLocation = locationDao.getLocationById(location.id)
        assertNotNull(fetchedLocation)
        assertEquals(true, fetchedLocation?.sync)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testDeleteAllLocations() = runTest {
        val location1 = LocationEntity(
            id = UUID.randomUUID(),
            latitude = 1.0,
            longitude = 1.0,
            timestamp = System.currentTimeMillis(),
            date = Date(),
            altitude = 1.0,
            speed = 1.0f,
            distance = 1.0f,
            sync = false
        )

        val location2 = LocationEntity(
            id = UUID.randomUUID(),
            latitude = 2.0,
            longitude = 2.0,
            timestamp = System.currentTimeMillis(),
            date = Date(),
            altitude = 2.0,
            speed = 2.0f,
            distance = 2.0f,
            sync = false
        )

        locationDao.insertLocationBatch(listOf(location1, location2))
        locationDao.deleteAllLocations()
        val allLocations = locationDao.getAllLocations()
        assertEquals(0, allLocations.size)
    }
}
