//package com.uoa.core.data
//
//import android.content.Context
//import androidx.room.Room
//import androidx.test.core.app.ApplicationProvider
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import com.uoa.core.Sdadb
//import com.uoa.core.database.daos.RawSensorDataDao
//import com.uoa.core.database.entities.RawSensorDataEntity
//import kotlinx.coroutines.runBlocking
//import app.cash.turbine.test
//import com.google.common.truth.Correspondence
//import com.google.common.truth.Truth.assertThat
//import kotlinx.coroutines.test.runTest
//import org.junit.After
//import org.junit.Before
//import org.junit.Test
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.runner.RunWith
//import java.time.Instant
//import java.time.ZoneId
//import java.util.Date
//import java.util.UUID
//
//@RunWith(AndroidJUnit4::class)
//class RawSensorDataDaoTest {
//
//    private lateinit var database: Sdadb
//    private lateinit var rawSensorDataDao: RawSensorDataDao
//
//    private val id: UUID = UUID.randomUUID()
//    private val sensorType = Math.random().toInt()
//    private val sensorTypeName = "Accelerometer"
//    private val values = listOf(1.2f, 3.4f, -0.5f)
//    private val timestamp = Instant.now()
//    private val date = Date(timestamp.toEpochMilli())
//    // Convert Long (epoch milliseconds) to LocalDate
//    val zoneId = ZoneId.systemDefault()
//
//    val sDate = Instant.ofEpochMilli(date.toInstant().toEpochMilli()).atZone(zoneId).toLocalDate()
//    private val accuracy = 3
//
//    private val entity = RawSensorDataEntity(id, sensorType, sensorTypeName, values, timestamp.toEpochMilli(), date, accuracy, null, null)
//
//    @Before
//    fun createDb() {
//        val context = ApplicationProvider.getApplicationContext<Context>()
//        database = Room.inMemoryDatabaseBuilder(
//            context, Sdadb::class.java
//        ).build()
//        rawSensorDataDao = database.rawSensorDataDao()
//    }
//
//    @After
//    fun closeDb() = database.close()
//
//    @Test
//    fun insertAndGetRawSensorDataById() = runTest {
//        // Create the expected entity
//        val expectedEntity = RawSensorDataEntity(
//            id = UUID.randomUUID(),
//            sensorType = Math.random().toInt(),
//            sensorTypeName = "Accelerometer",
//            values = listOf(1.2f, 3.4f, -0.5f),
//            timestamp = Instant.now().toEpochMilli(),
//            date= Date(timestamp.toEpochMilli()),
//            accuracy = 3,
//            locationId = null,
//            tripId = null,
//            sync = false
//        )
//
//        // Insert the entity into the database
//        rawSensorDataDao.insertRawSensorData(expectedEntity)
//
//        // Retrieve the entity from the database
//        val retrievedEntity = rawSensorDataDao.getRawSensorDataById(expectedEntity.id)
//
//        // Custom Correspondence for RawSensorDataEntity
//        val rawSensorDataCorrespondence = Correspondence.from<RawSensorDataEntity, RawSensorDataEntity>(
//            { actual, expected ->
//                actual.id == expected.id &&
//                        actual.sensorType == expected.sensorType &&
//                        actual.sensorTypeName == expected.sensorTypeName &&
//                        actual.values == expected.values &&
//                        actual.timestamp == expected.timestamp &&
//                        actual.accuracy == expected.accuracy &&
//                        actual.locationId == expected.locationId &&
//                        actual.tripId == expected.tripId &&
//                        actual.sync == expected.sync
//            },
//            "is equivalent to"
//        )
//
//        // Assertion using usingCorrespondence (with Subject)
//        val subject = retrievedEntity?.let { rawSensorDataCorrespondence.compare(expectedEntity, it) }
//        subject?.let { assertThat(it).isTrue() }
//
//
//    }
//
//    @Test
//    fun getRawSensorDataByTripId() = runTest {
////        val tripId = UUID.randomUUID()
//        val entity1 = RawSensorDataEntity(
//            id = UUID.randomUUID(),
//            sensorType = Math.random().toInt(),
//            sensorTypeName = "Accelerometer",
//            values = listOf(1.2f, 3.4f, -0.5f),
//            timestamp = Instant.now().toEpochMilli(),
//            date= Date(timestamp.toEpochMilli()),
//            accuracy = 3,
//            locationId = null,
//            tripId = null,
//            sync = false
//        )
//        val entity2 = RawSensorDataEntity(
//            id = UUID.randomUUID(),
//            sensorType = Math.random().toInt(),
//            sensorTypeName = "Accelerometer",
//            values = listOf(1.2f, 3.4f, -0.5f),
//            timestamp = Instant.now().toEpochMilli(),
//            date= Date(timestamp.toEpochMilli()),
//            accuracy = 3,
//            locationId = null,
//            tripId = null,
//            sync = false
//        )
//        val entities = listOf(entity1,entity2)
//        rawSensorDataDao.insertRawSensorDataBatch(entities)
//
//        entity2.tripId?.let {
//            rawSensorDataDao.getSensorDataByTripId(it).test {
//    //            // Ensure the correct entities are emitted
//                assertEquals(entities, awaitItem())
//                awaitComplete()
//            }
//        }
//    }
//
//    @Test
//    fun getRawSensorDataBetweenDates() = runTest {
//
//        val rawSensorDataEntityCorrespondence: Correspondence<RawSensorDataEntity, RawSensorDataEntity> =
//            Correspondence.from(
//                { actual, expected ->
//                    actual.id == expected.id &&
//                            actual.sensorType == expected.sensorType &&
//                            actual.sensorTypeName == expected.sensorTypeName &&
//                            actual.values == expected.values &&
//                            actual.timestamp == expected.timestamp &&
//                            actual.accuracy == expected.accuracy &&
//                            actual.locationId == expected.locationId &&
//                            actual.tripId == expected.tripId &&
//                            actual.sync == expected.sync
//                },
//                "is equivalent to"
//            )
//
//        val startDate = Date(System.currentTimeMillis() - 60000) // 1 minute ago
//        val endDate = Date() // Current time
//
//        val expectedEntity1 = RawSensorDataEntity(
//            UUID.randomUUID(), 0, "Accelerometer", listOf(1.2f, 3.4f, -0.5f),
//            startDate.toInstant().toEpochMilli(), startDate, 3, null, null
//        )
//        val expectedEntity2 = RawSensorDataEntity(
//            UUID.randomUUID(), 0, "Accelerometer", listOf(1.2f, 3.4f, -0.5f),
//            endDate.toInstant().toEpochMilli(), endDate, 3, null, null
//        )
//        rawSensorDataDao.insertRawSensorDataBatch(listOf(expectedEntity1, expectedEntity2))
//
//        // Convert Long (epoch milliseconds) to LocalDate
//        val zoneId = ZoneId.systemDefault()
//        val sDate = Instant.ofEpochMilli(startDate.toInstant().toEpochMilli()).atZone(zoneId).toLocalDate()
//        val eDate = Instant.ofEpochMilli(endDate.toInstant().toEpochMilli()).atZone(zoneId).toLocalDate()
//        rawSensorDataDao.getSensorDataBetweenDates(sDate, eDate).test {
//            // Ensure the correct entities are emitted
//            val actualEntities = awaitItem()
//            assertThat((actualEntities))
//                .comparingElementsUsing(rawSensorDataEntityCorrespondence)
//                .containsExactly(expectedEntity1, expectedEntity2);
//        }
//    }
//
//
//    @Test
//    fun updateRawSensorData() = runBlocking {
//        rawSensorDataDao.insertRawSensorData(entity)
//
//        // Modify the entity
//        entity.sync = true
//
//        rawSensorDataDao.updateRawSensorData(entity)
//        val updatedEntity = rawSensorDataDao.getRawSensorDataById(entity.id)
//
//        assertEquals(true, updatedEntity?.sync) // Check if the update was successful
//    }
//
//// ... similar tests for other DAO methods (delete, getUnsynced, etc.)
//
//}
