//package com.uoa.core.data
//import androidx.room.Room
//import androidx.test.core.app.ApplicationProvider
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import com.uoa.core.Sdadb
//import com.uoa.core.database.daos.TripDao
//import com.uoa.core.database.entities.TripEntity
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.test.runTest
//import org.junit.After
//import org.junit.Before
//import org.junit.Test
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Assertions.assertNotNull
//import org.junit.jupiter.api.Assertions.assertNull
//import org.junit.runner.RunWith
//import java.util.*
//
//@RunWith(AndroidJUnit4::class)
//class TripDaoTest {
//
//    private lateinit var database: Sdadb
//    private lateinit var tripDao: TripDao
//
//    @Before
//    fun setUp() {
//        database = Room.inMemoryDatabaseBuilder(
//            ApplicationProvider.getApplicationContext(),
//            Sdadb::class.java
//        ).allowMainThreadQueries().build()
//        tripDao = database.tripDao()
//    }
//
//    @After
//    fun tearDown() {
//        database.close()
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testInsertAndGetTrip() = runTest {
//        val trip = TripEntity(
//            id = UUID.randomUUID(),
//            driverProfileId = UUID.randomUUID(),
//            startDate = Date(),
//            endDate = null,
//            startTime = System.currentTimeMillis(),
//            endTime = null,
//            synced = false
//        )
//
//        tripDao.insertTrip(trip)
//        val fetchedTrip = tripDao.getTripById(trip.id)
//        assertNotNull(fetchedTrip)
//        assertEquals(trip.id, fetchedTrip?.id)
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testUpdateTrip() = runTest {
//        val trip = TripEntity(
//            id = UUID.randomUUID(),
//            driverProfileId = UUID.randomUUID(),
//            startDate = Date(),
//            endDate = null,
//            startTime = System.currentTimeMillis(),
//            endTime = null,
//            synced = false
//        )
//
//        tripDao.insertTrip(trip)
//        val updatedEndTime = System.currentTimeMillis()
//        val updatedTrip = trip.copy(endTime = updatedEndTime, synced = true)
//        tripDao.updateTrip(updatedTrip)
//
//        val fetchedTrip = tripDao.getTripById(trip.id)
//        assertNotNull(fetchedTrip)
//        assertEquals(updatedEndTime, fetchedTrip?.endTime)
//        assertEquals(true, fetchedTrip?.synced)
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testGetAllTrips() = runTest {
//        val trip1 = TripEntity(
//            id = UUID.randomUUID(),
//            driverProfileId = UUID.randomUUID(),
//            startDate = Date(),
//            endDate = null,
//            startTime = System.currentTimeMillis(),
//            endTime = null,
//            synced = false
//        )
//
//        val trip2 = TripEntity(
//            id = UUID.randomUUID(),
//            driverProfileId = UUID.randomUUID(),
//            startDate = Date(),
//            endDate = null,
//            startTime = System.currentTimeMillis(),
//            endTime = null,
//            synced = false
//        )
//
//        tripDao.insertTrip(trip1)
//        tripDao.insertTrip(trip2)
//
//        val allTrips = tripDao.getAllTrips()
//        assertEquals(2, allTrips.size)
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testGetTripsByDriverProfileId() = runTest {
//        val trip1 = TripEntity(
//            id = UUID.randomUUID(),
//            driverProfileId = UUID.randomUUID(),
//            startDate = Date(),
//            endDate = null,
//            startTime = System.currentTimeMillis(),
//            endTime = null,
//            synced = false
//        )
//
//        val trip2 = TripEntity(
//            id = UUID.randomUUID(),
//            driverProfileId = UUID.randomUUID(),
//            startDate = Date(),
//            endDate = null,
//            startTime = System.currentTimeMillis(),
//            endTime = null,
//            synced = false
//        )
//
//        tripDao.insertTrip(trip1)
//        tripDao.insertTrip(trip2)
//
//        val trips = tripDao.getTripsByDriverProfileId(trip1.driverProfileId!!)
//        assertEquals(1, trips.size)
//        assertEquals(trip1.id, trips[0].id)
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testGetTripsBySyncStatus() = runTest {
//        val trip1 = TripEntity(
//            id = UUID.randomUUID(),
//            driverProfileId = UUID.randomUUID(),
//            startDate = Date(),
//            endDate = null,
//            startTime = System.currentTimeMillis(),
//            endTime = null,
//            synced = false
//        )
//
//        val trip2 = TripEntity(
//            id = UUID.randomUUID(),
//            driverProfileId = UUID.randomUUID(),
//            startDate = Date(),
//            endDate = null,
//            startTime = System.currentTimeMillis(),
//            endTime = null,
//            synced = true
//        )
//
//        tripDao.insertTrip(trip1)
//        tripDao.insertTrip(trip2)
//
//        val unsyncedTrips = tripDao.getTripsBySyncStatus(false)
//        val syncedTrips = tripDao.getTripsBySyncStatus(true)
//
//        assertEquals(1, unsyncedTrips.size)
//        assertEquals(trip1.id, unsyncedTrips[0].id)
//
//        assertEquals(1, syncedTrips.size)
//        assertEquals(trip2.id, syncedTrips[0].id)
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testDeleteTripById() = runTest {
//        val trip = TripEntity(
//            id = UUID.randomUUID(),
//            driverProfileId = UUID.randomUUID(),
//            startDate = Date(),
//            endDate = null,
//            startTime = System.currentTimeMillis(),
//            endTime = null,
//            synced = false
//        )
//
//        tripDao.insertTrip(trip)
//        tripDao.deleteTripById(trip.id)
//        val fetchedTrip = tripDao.getTripById(trip.id)
//        assertNull(fetchedTrip)
//    }
//}
