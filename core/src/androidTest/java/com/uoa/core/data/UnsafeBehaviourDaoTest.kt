//package com.uoa.core.data
//
//import androidx.room.Room
//import androidx.test.core.app.ApplicationProvider
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import com.uoa.core.Sdadb
//import com.uoa.core.database.daos.LocationDao
//import com.uoa.core.database.daos.TripDao
//import com.uoa.core.database.daos.UnsafeBehaviourDao
//import com.uoa.core.database.entities.LocationEntity
//import com.uoa.core.database.entities.TripEntity
//import com.uoa.core.database.entities.UnsafeBehaviourEntity
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.test.runTest
//import org.junit.After
//import org.junit.Before
//import org.junit.Test
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Assertions.assertNotNull
//import org.junit.jupiter.api.Assertions.assertNull
//import org.junit.runner.RunWith
//import java.time.Instant
//import java.time.ZoneId
//import java.util.*
//
//@RunWith(AndroidJUnit4::class)
//class UnsafeBehaviourDaoTest {
//
//    private lateinit var database: Sdadb
//    private lateinit var unsafeBehaviourDao: UnsafeBehaviourDao
//    private lateinit var tripDao: TripDao
//    private lateinit var locationDao: LocationDao
//
//    @Before
//    fun setUp() {
//        database = Room.inMemoryDatabaseBuilder(
//            ApplicationProvider.getApplicationContext(),
//            Sdadb::class.java
//        ).allowMainThreadQueries().build()
//        unsafeBehaviourDao = database.unsafeBehaviourDao()
//        tripDao = database.tripDao()
//        locationDao = database.locationDataDao()
//    }
//
//    @After
//    fun tearDown() {
//        database.close()
//    }
//
//    private suspend fun insertTripRecord(tripId: UUID) {
//        val trip = TripEntity(
//            id = tripId,
//            driverProfileId =UUID.randomUUID() ,
//            startDate = Date(),
//            endDate = Date(),
//            startTime = System.currentTimeMillis(),
//            endTime = System.currentTimeMillis(),
//            synced = false
//        )
//        tripDao.insertTrip(trip)
//    }
//
//    private suspend fun insertLocationRecord(locationId: UUID) {
//        val location = LocationEntity(
//            id = locationId,
//            latitude = 1.0,
//            longitude = 1.0,
//            timestamp = System.currentTimeMillis(),
//            date = Date(),
//            altitude = 1.0,
//            speed = 1.0f,
//            distance = 1.0f,
//            sync = false
//        )
//        locationDao.insertLocation(location)
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testInsertAndGetUnsafeBehaviour() = runTest {
//        val tripId = UUID.randomUUID()
//        val locationId = UUID.randomUUID()
//        insertTripRecord(tripId)
//        insertLocationRecord(locationId)
//
//        val unsafeBehaviour = UnsafeBehaviourEntity(
//            id = UUID.randomUUID(),
//            tripId = tripId,
//            locationId = locationId,
//            behaviorType = "Speeding",
//            severity = 5.0f,
//            timestamp = System.currentTimeMillis(),
//            date = Date(),
//            synced = false,
//            cause = "Over speed limit"
//        )
//
//        unsafeBehaviourDao.insertUnsafeBehaviour(unsafeBehaviour)
//        val fetchedBehaviour = unsafeBehaviourDao.getUnsafeBehaviourById(unsafeBehaviour.id)
//        assertNotNull(fetchedBehaviour)
//        assertEquals(unsafeBehaviour.id, fetchedBehaviour?.id)
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testInsertUnsafeBehaviourBatch() = runTest {
//        val tripId1 = UUID.randomUUID()
//        val locationId1 = UUID.randomUUID()
//        insertTripRecord(tripId1)
//        insertLocationRecord(locationId1)
//
//        val tripId2 = UUID.randomUUID()
//        val locationId2 = UUID.randomUUID()
//        insertTripRecord(tripId2)
//        insertLocationRecord(locationId2)
//
//        val unsafeBehaviour1 = UnsafeBehaviourEntity(
//            id = UUID.randomUUID(),
//            tripId = tripId1,
//            locationId = locationId1,
//            behaviorType = "Speeding",
//            severity = 5.0f,
//            timestamp = System.currentTimeMillis(),
//            date = Date(),
//            synced = false,
//            cause = "Over speed limit"
//        )
//
//        val unsafeBehaviour2 = UnsafeBehaviourEntity(
//            id = UUID.randomUUID(),
//            tripId = tripId2,
//            locationId = locationId2,
//            behaviorType = "Harsh Braking",
//            severity = 3.0f,
//            timestamp = System.currentTimeMillis(),
//            date = Date(),
//            synced = false,
//            cause = "Sudden brake"
//        )
//
//        unsafeBehaviourDao.insertUnsafeBehaviourBatch(listOf(unsafeBehaviour1, unsafeBehaviour2))
//        val allUnsafeBehaviours = unsafeBehaviourDao.getUnsafeBehavioursBySyncStatus(false)
//        assertEquals(2, allUnsafeBehaviours.size)
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testGetUnsafeBehavioursByTripId() = runTest {
//        val tripId = UUID.randomUUID()
//        val locationId1 = UUID.randomUUID()
//        val locationId2 = UUID.randomUUID()
//        insertTripRecord(tripId)
//        insertLocationRecord(locationId1)
//        insertLocationRecord(locationId2)
//
//        val unsafeBehaviour1 = UnsafeBehaviourEntity(
//            id = UUID.randomUUID(),
//            tripId = tripId,
//            locationId = locationId1,
//            behaviorType = "Speeding",
//            severity = 5.0f,
//            timestamp = System.currentTimeMillis(),
//            date = Date(),
//            synced = false,
//            cause = "Over speed limit"
//        )
//
//        val unsafeBehaviour2 = UnsafeBehaviourEntity(
//            id = UUID.randomUUID(),
//            tripId = tripId,
//            locationId = locationId2,
//            behaviorType = "Harsh Braking",
//            severity = 3.0f,
//            timestamp = System.currentTimeMillis(),
//            date = Date(),
//            synced = false,
//            cause = "Sudden brake"
//        )
//
//        unsafeBehaviourDao.insertUnsafeBehaviourBatch(listOf(unsafeBehaviour1, unsafeBehaviour2))
//
//        val unsafeBehaviours = unsafeBehaviourDao.getUnsafeBehavioursByTripId(tripId).first()
//        assertEquals(2, unsafeBehaviours.size)
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testUpdateUnsafeBehaviour() = runTest {
//        val tripId = UUID.randomUUID()
//        val locationId = UUID.randomUUID()
//        insertTripRecord(tripId)
//        insertLocationRecord(locationId)
//
//        val unsafeBehaviour = UnsafeBehaviourEntity(
//            id = UUID.randomUUID(),
//            tripId = tripId,
//            locationId = locationId,
//            behaviorType = "Speeding",
//            severity = 5.0f,
//            timestamp = System.currentTimeMillis(),
//            date = Date(),
//            synced = false,
//            cause = "Over speed limit"
//        )
//
//        unsafeBehaviourDao.insertUnsafeBehaviour(unsafeBehaviour)
//        val updatedBehaviour = unsafeBehaviour.copy(synced = true)
//        unsafeBehaviourDao.updateUnsafeBehaviour(updatedBehaviour)
//
//        val fetchedBehaviour = unsafeBehaviourDao.getUnsafeBehaviourById(unsafeBehaviour.id)
//        assertNotNull(fetchedBehaviour)
//        assertEquals(true, fetchedBehaviour?.synced)
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testDeleteUnsafeBehaviour() = runTest {
//        val tripId = UUID.randomUUID()
//        val locationId = UUID.randomUUID()
//        insertTripRecord(tripId)
//        insertLocationRecord(locationId)
//
//        val unsafeBehaviour = UnsafeBehaviourEntity(
//            id = UUID.randomUUID(),
//            tripId = tripId,
//            locationId = locationId,
//            behaviorType = "Speeding",
//            severity = 5.0f,
//            timestamp = System.currentTimeMillis(),
//            date = Date(),
//            synced = false,
//            cause = "Over speed limit"
//        )
//
//        unsafeBehaviourDao.insertUnsafeBehaviour(unsafeBehaviour)
//        unsafeBehaviourDao.deleteUnsafeBehaviour(unsafeBehaviour)
//        val fetchedBehaviour = unsafeBehaviourDao.getUnsafeBehaviourById(unsafeBehaviour.id)
//        assertNull(fetchedBehaviour)
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testGetUnsafeBehavioursBySyncStatus() = runTest {
//        val tripId1 = UUID.randomUUID()
//        val locationId1 = UUID.randomUUID()
//        insertTripRecord(tripId1)
//        insertLocationRecord(locationId1)
//
//        val tripId2 = UUID.randomUUID()
//        val locationId2 = UUID.randomUUID()
//        insertTripRecord(tripId2)
//        insertLocationRecord(locationId2)
//
//        val unsafeBehaviour1 = UnsafeBehaviourEntity(
//            id = UUID.randomUUID(),
//            tripId = tripId1,
//            locationId = locationId1,
//            behaviorType = "Speeding",
//            severity = 5.0f,
//            timestamp = System.currentTimeMillis(),
//            date = Date(),
//            synced = false,
//            cause = "Over speed limit"
//        )
//
//        val unsafeBehaviour2 = UnsafeBehaviourEntity(
//            id = UUID.randomUUID(),
//            tripId = tripId2,
//            locationId = locationId2,
//            behaviorType = "Harsh Braking",
//            severity = 3.0f,
//            timestamp = System.currentTimeMillis(),
//            date = Date(),
//            synced = true,
//            cause = "Sudden brake"
//        )
//
//        unsafeBehaviourDao.insertUnsafeBehaviourBatch(listOf(unsafeBehaviour1, unsafeBehaviour2))
//        val unsyncedBehaviours = unsafeBehaviourDao.getUnsafeBehavioursBySyncStatus(false)
//        assertEquals(1, unsyncedBehaviours.size)
//        assertEquals(false, unsyncedBehaviours[0].synced)
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testDeleteAllUnsafeBehavioursBySyncStatus() = runTest {
//        val tripId1 = UUID.randomUUID()
//        val locationId1 = UUID.randomUUID()
//        insertTripRecord(tripId1)
//        insertLocationRecord(locationId1)
//
//        val tripId2 = UUID.randomUUID()
//        val locationId2 = UUID.randomUUID()
//        insertTripRecord(tripId2)
//        insertLocationRecord(locationId2)
//
//        val unsafeBehaviour1 = UnsafeBehaviourEntity(
//            id = UUID.randomUUID(),
//            tripId = tripId1,
//            locationId = locationId1,
//            behaviorType = "Speeding",
//            severity = 5.0f,
//            timestamp = System.currentTimeMillis(),
//            date = Date(),
//            synced = false,
//            cause = "Over speed limit"
//        )
//
//        val unsafeBehaviour2 = UnsafeBehaviourEntity(
//            id = UUID.randomUUID(),
//            tripId = tripId2,
//            locationId = locationId2,
//            behaviorType = "Harsh Braking",
//            severity = 3.0f,
//            timestamp = System.currentTimeMillis(),
//            date = Date(),
//            synced = true,
//            cause = "Sudden brake"
//        )
//
//        unsafeBehaviourDao.insertUnsafeBehaviourBatch(listOf(unsafeBehaviour1, unsafeBehaviour2))
//        unsafeBehaviourDao.deleteAllUnsafeBehavioursBySyncStatus(false)
//
//        val remainingBehaviours = unsafeBehaviourDao.getUnsafeBehavioursBySyncStatus(false)
//        assertEquals(0, remainingBehaviours.size)
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testGetUnsafeBehaviourCountByTypeAndTime() = runTest {
//        val tripId = UUID.randomUUID()
//        val locationId = UUID.randomUUID()
//        insertTripRecord(tripId)
//        insertLocationRecord(locationId)
//
//        val unsafeBehaviour = UnsafeBehaviourEntity(
//            id = UUID.randomUUID(),
//            tripId = tripId,
//            locationId = locationId,
//            behaviorType = "Speeding",
//            severity = 5.0f,
//            timestamp = System.currentTimeMillis(),
//            date = Date(),
//            synced = false,
//            cause = "Over speed limit"
//        )
//
//        unsafeBehaviourDao.insertUnsafeBehaviour(unsafeBehaviour)
//
//        val count = unsafeBehaviourDao.getUnsafeBehaviourCountByTypeAndTime(
//            "Speeding",
//            System.currentTimeMillis() - 1000,
//            System.currentTimeMillis() + 1000
//        )
//        assertEquals(1, count)
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testGetUnsafeBehaviourCountByTypeAndDistance() = runTest {
//        val tripId = UUID.randomUUID()
//        val locationId = UUID.randomUUID()
//        insertTripRecord(tripId)
//        insertLocationRecord(locationId)
//
//        val unsafeBehaviour = UnsafeBehaviourEntity(
//            id = UUID.randomUUID(),
//            tripId = tripId,
//            locationId = locationId,
//            behaviorType = "Speeding",
//            severity = 5.0f,
//            timestamp = System.currentTimeMillis(),
//            date = Date(),
//            synced = false,
//            cause = "Over speed limit"
//        )
//
//        unsafeBehaviourDao.insertUnsafeBehaviour(unsafeBehaviour)
//        val count = unsafeBehaviourDao.getUnsafeBehaviourCountByTypeAndDistance("Speeding", unsafeBehaviour.tripId, 1000f)
//        assertEquals(1, count)
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun testGetUnsafeBehavioursBetweenDates() = runTest {
//        val tripId = UUID.randomUUID()
//        val locationId = UUID.randomUUID()
//        insertTripRecord(tripId)
//        insertLocationRecord(locationId)
//
//        val unsafeBehaviour = UnsafeBehaviourEntity(
//            id = UUID.randomUUID(),
//            tripId = tripId,
//            locationId = locationId,
//            behaviorType = "Speeding",
//            severity = 5.0f,
//            timestamp = System.currentTimeMillis(),
//            date = Date(),
//            synced = false,
//            cause = "Over speed limit"
//        )
//
//        unsafeBehaviourDao.insertUnsafeBehaviour(unsafeBehaviour)
//
//        val startDate = Date(System.currentTimeMillis() - 1000)
//        val endDate = Date(System.currentTimeMillis() + 1000)
//        // Convert Long (epoch milliseconds) to LocalDate
//        val zoneId = ZoneId.systemDefault()
//        val sDate = Instant.ofEpochMilli(startDate.toInstant().toEpochMilli()).atZone(zoneId).toLocalDate()
//        val eDate = Instant.ofEpochMilli(endDate.toInstant().toEpochMilli()).atZone(zoneId).toLocalDate()
//
//        val unsafeBehaviours = unsafeBehaviourDao.getUnsafeBehavioursBetweenDates(sDate, eDate).first()
//        assertEquals(1, unsafeBehaviours.size)
//        assertEquals(unsafeBehaviour.id, unsafeBehaviours[0].id)
//    }
//}
