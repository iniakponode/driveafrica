package com.uoa.core.behaviouranalysis

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.uoa.core.database.entities.LocationEntity
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.model.LocationData
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewUnsafeDrivingBehaviourAnalyserTest {

    @Test
    fun speedingDetectedWithLocationSpeedLimit() = runBlocking {
        val locationId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val driverId = UUID.randomUUID()
        val locationEntity = LocationEntity(
            id = locationId,
            latitude = 0.0,
            longitude = 0.0,
            timestamp = 1_000L,
            date = Date(1_000L),
            altitude = 0.0,
            accuracy = 12.0,
            speed = 30f,
            distance = 0f,
            speedLimit = 20.0,
            processed = false,
            sync = false
        )
        val repository = FakeLocationRepository(mapOf(locationId to locationEntity))
        val analyser = NewUnsafeDrivingBehaviourAnalyser(repository)
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val dataStart = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = 0,
            sensorTypeName = "Speed",
            values = listOf(0f, 0f, 0f),
            timestamp = 1_000L,
            date = Date(1_000L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )
        val dataEnd = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = 0,
            sensorTypeName = "Speed",
            values = listOf(0f, 0f, 0f),
            timestamp = 21_500L,
            date = Date(21_500L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )

        val results = analyser.analyze(flowOf(dataStart, dataEnd), context).toList()

        assertEquals(1, results.size)
        val behavior = results.first()
        assertEquals("Speeding", behavior.behaviorType)
        assertEquals(tripId, behavior.tripId)
        assertEquals(locationId, behavior.locationId)
        assertTrue("Expected positive severity for speeding", behavior.severity > 0f)
    }

    @Test
    fun speedingNotTriggeredBelowTolerance() = runBlocking {
        val locationId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val driverId = UUID.randomUUID()
        val locationEntity = LocationEntity(
            id = locationId,
            latitude = 0.0,
            longitude = 0.0,
            timestamp = 1_000L,
            date = Date(1_000L),
            altitude = 0.0,
            accuracy = 12.0,
            speed = 21.5f,
            distance = 0f,
            speedLimit = 20.0,
            processed = false,
            sync = false
        )
        val repository = FakeLocationRepository(mapOf(locationId to locationEntity))
        val analyser = NewUnsafeDrivingBehaviourAnalyser(repository)
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val dataStart = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = 0,
            sensorTypeName = "Speed",
            values = listOf(0f, 0f, 0f),
            timestamp = 1_000L,
            date = Date(1_000L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )
        val dataEnd = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = 0,
            sensorTypeName = "Speed",
            values = listOf(0f, 0f, 0f),
            timestamp = 21_500L,
            date = Date(21_500L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )

        val results = analyser.analyze(flowOf(dataStart, dataEnd), context).toList()

        assertTrue("Expected no speeding below tolerance threshold", results.isEmpty())
    }

    @Test
    fun noBehaviorWhenTripIdMissing() = runBlocking {
        val locationId = UUID.randomUUID()
        val driverId = UUID.randomUUID()
        val locationEntity = LocationEntity(
            id = locationId,
            latitude = 0.0,
            longitude = 0.0,
            timestamp = 2_000L,
            date = Date(2_000L),
            altitude = 0.0,
            accuracy = 12.0,
            speed = 30f,
            distance = 0f,
            speedLimit = 20.0,
            processed = false,
            sync = false
        )
        val repository = FakeLocationRepository(mapOf(locationId to locationEntity))
        val analyser = NewUnsafeDrivingBehaviourAnalyser(repository)
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val data = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = 0,
            sensorTypeName = "Speed",
            values = listOf(0f, 0f, 0f),
            timestamp = 2_000L,
            date = Date(2_000L),
            accuracy = 3,
            locationId = locationId,
            tripId = null,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )

        val results = analyser.analyze(flowOf(data), context).toList()

        assertTrue(results.isEmpty())
    }

    @Test
    fun aggressiveStopAndGoDetectedWithLinearAccelerationIncrease() = runBlocking {
        val locationId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val driverId = UUID.randomUUID()
        val locationId2 = UUID.randomUUID()
        val locationId3 = UUID.randomUUID()
        val locationEntity = LocationEntity(
            id = locationId,
            latitude = 0.0000,
            longitude = 0.0000,
            timestamp = 4_500L,
            date = Date(4_500L),
            altitude = 0.0,
            accuracy = 10.0,
            speed = 15f, // 15 m/s (54 km/h) - above MIN_ACCELERATION_SPEED_MPS
            distance = 0f,
            speedLimit = 50.0,
            processed = false,
            sync = false
        )
        val locationEntity2 = LocationEntity(
            id = locationId2,
            latitude = 0.0001, // ~11m north
            longitude = 0.0000,
            timestamp = 4_700L,
            date = Date(4_700L),
            altitude = 0.0,
            accuracy = 10.0,
            speed = 15f,
            distance = 5f, // Clear movement for heading calc
            speedLimit = 50.0,
            processed = false,
            sync = false
        )
        val locationEntity3 = LocationEntity(
            id = locationId3,
            latitude = 0.0002, // ~22m north
            longitude = 0.0000,
            timestamp = 5_100L,
            date = Date(5_100L),
            altitude = 0.0,
            accuracy = 10.0,
            speed = 15f,
            distance = 5f,
            speedLimit = 50.0,
            processed = false,
            sync = false
        )
        val repository = FakeLocationRepository(
            mapOf(
                locationId to locationEntity,
                locationId2 to locationEntity2,
                locationId3 to locationEntity3
            )
        )
        val analyser = NewUnsafeDrivingBehaviourAnalyser(repository)
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Seed samples to establish heading before acceleration events
        val locSeed1 = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.LINEAR_ACCELERATION_TYPE,
            sensorTypeName = "Linear Acceleration",
            values = listOf(0f, 0.1f, 0f), // Minimal acceleration
            timestamp = 4_500L,
            date = Date(4_500L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )
        val locSeed2 = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.LINEAR_ACCELERATION_TYPE,
            sensorTypeName = "Linear Acceleration",
            values = listOf(0f, 0.1f, 0f), // Minimal acceleration
            timestamp = 4_650L,
            date = Date(4_650L),
            accuracy = 3,
            locationId = locationId2,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )
        val dataStart = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.LINEAR_ACCELERATION_TYPE,
            sensorTypeName = "Linear Acceleration",
            values = listOf(0f, 2.0f, 0f),
            timestamp = 4_800L,
            date = Date(4_800L),
            accuracy = 3,
            locationId = locationId2,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )
        val dataMid = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.LINEAR_ACCELERATION_TYPE,
            sensorTypeName = "Linear Acceleration",
            values = listOf(0f, -2.0f, 0f),
            timestamp = 4_900L,
            date = Date(4_900L),
            accuracy = 3,
            locationId = locationId2,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )
        val dataFollowUp = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.LINEAR_ACCELERATION_TYPE,
            sensorTypeName = "Linear Acceleration",
            values = listOf(0f, 2.0f, 0f),
            timestamp = 5_100L,
            date = Date(5_100L),
            accuracy = 3,
            locationId = locationId3,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )

        val results = analyser.analyze(flowOf(locSeed1, locSeed2, dataStart, dataMid, dataFollowUp), context).toList()

        assertEquals(1, results.size)
        val behavior = results.first()
        assertEquals("Aggressive Stop-and-Go", behavior.behaviorType)
        assertEquals(tripId, behavior.tripId)
        assertEquals(locationId3, behavior.locationId)
    }

    @Test
    fun aggressiveStopAndGoDetectedWithLinearAccelerationDecrease() = runBlocking {
        val locationId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val driverId = UUID.randomUUID()
        val locationId2 = UUID.randomUUID()
        val locationId3 = UUID.randomUUID()
        val locationEntity = LocationEntity(
            id = locationId,
            latitude = 0.0000,
            longitude = 0.0000,
            timestamp = 4_500L,
            date = Date(4_500L),
            altitude = 0.0,
            accuracy = 10.0,
            speed = 15f, // 15 m/s (54 km/h) - above MIN_ACCELERATION_SPEED_MPS
            distance = 0f,
            speedLimit = 50.0,
            processed = false,
            sync = false
        )
        val locationEntity2 = LocationEntity(
            id = locationId2,
            latitude = 0.0001, // ~11m north
            longitude = 0.0000,
            timestamp = 4_700L,
            date = Date(4_700L),
            altitude = 0.0,
            accuracy = 10.0,
            speed = 15f,
            distance = 5f,
            speedLimit = 50.0,
            processed = false,
            sync = false
        )
        val locationEntity3 = LocationEntity(
            id = locationId3,
            latitude = 0.0002, // ~22m north
            longitude = 0.0000,
            timestamp = 5_100L,
            date = Date(5_100L),
            altitude = 0.0,
            accuracy = 10.0,
            speed = 15f,
            distance = 5f,
            speedLimit = 50.0,
            processed = false,
            sync = false
        )
        val repository = FakeLocationRepository(
            mapOf(
                locationId to locationEntity,
                locationId2 to locationEntity2,
                locationId3 to locationEntity3
            )
        )
        val analyser = NewUnsafeDrivingBehaviourAnalyser(repository)
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Seed samples to establish heading before braking events
        val locSeed1 = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.LINEAR_ACCELERATION_TYPE,
            sensorTypeName = "Linear Acceleration",
            values = listOf(0f, 0.1f, 0f), // Minimal acceleration
            timestamp = 4_500L,
            date = Date(4_500L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )
        val locSeed2 = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.LINEAR_ACCELERATION_TYPE,
            sensorTypeName = "Linear Acceleration",
            values = listOf(0f, 0.1f, 0f), // Minimal acceleration
            timestamp = 4_650L,
            date = Date(4_650L),
            accuracy = 3,
            locationId = locationId2,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )
        val dataStart = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.LINEAR_ACCELERATION_TYPE,
            sensorTypeName = "Linear Acceleration",
            values = listOf(0f, -2.0f, 0f),
            timestamp = 4_800L,
            date = Date(4_800L),
            accuracy = 3,
            locationId = locationId2,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )
        val dataMid = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.LINEAR_ACCELERATION_TYPE,
            sensorTypeName = "Linear Acceleration",
            values = listOf(0f, 2.0f, 0f),
            timestamp = 4_900L,
            date = Date(4_900L),
            accuracy = 3,
            locationId = locationId2,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )
        val dataFollowUp = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.LINEAR_ACCELERATION_TYPE,
            sensorTypeName = "Linear Acceleration",
            values = listOf(0f, -2.0f, 0f),
            timestamp = 5_100L,
            date = Date(5_100L),
            accuracy = 3,
            locationId = locationId3,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )

        val results = analyser.analyze(flowOf(locSeed1, locSeed2, dataStart, dataMid, dataFollowUp), context).toList()

        assertEquals(1, results.size)
        val behavior = results.first()
        assertEquals("Aggressive Stop-and-Go", behavior.behaviorType)
        assertEquals(tripId, behavior.tripId)
        assertEquals(locationId3, behavior.locationId)
    }

    @Test
    fun swervingDetectedWithRotationVector() = runBlocking {
        val locationId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val driverId = UUID.randomUUID()
        val locationEntity = LocationEntity(
            id = locationId,
            latitude = 0.0,
            longitude = 0.0,
            timestamp = 5_000L,
            date = Date(5_000L),
            altitude = 0.0,
            accuracy = 12.0,
            speed = 12f,
            distance = 6f,
            speedLimit = 20.0,
            processed = false,
            sync = false
        )
        val repository = FakeLocationRepository(mapOf(locationId to locationEntity))
        val analyser = NewUnsafeDrivingBehaviourAnalyser(repository)
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val seed = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = 0,
            sensorTypeName = "Seed",
            values = listOf(0f, 0f, 0f),
            timestamp = 4_900L,
            date = Date(4_900L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )
        val data = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.GYROSCOPE_TYPE,
            sensorTypeName = "Gyroscope",
            values = listOf(0f, 0f, 0.2f),
            timestamp = 5_000L,
            date = Date(5_000L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )

        val dataFollowUp = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.GYROSCOPE_TYPE,
            sensorTypeName = "Gyroscope",
            values = listOf(0f, 0f, -0.2f),
            timestamp = 5_200L,
            date = Date(5_200L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )
        val dataFinal = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.GYROSCOPE_TYPE,
            sensorTypeName = "Gyroscope",
            values = listOf(0f, 0f, 0.2f),
            timestamp = 5_400L,
            date = Date(5_400L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )

        val results = analyser.analyze(flowOf(seed, data, dataFollowUp, dataFinal), context).toList()

        assertEquals(1, results.size)
        val behavior = results.first()
        assertEquals("Swerving", behavior.behaviorType)
        assertEquals(tripId, behavior.tripId)
        assertEquals(locationId, behavior.locationId)
    }

    private class FakeLocationRepository(
        private val locations: Map<UUID, LocationEntity>
    ) : LocationRepository {
        override suspend fun insertLocation(location: LocationEntity) = Unit
        override suspend fun getLocationsByIds(ids: List<UUID>): List<LocationData> = emptyList()
        override suspend fun insertLocationBatch(locationList: List<LocationEntity>) = Unit
        override suspend fun getLocationById(id: UUID): LocationEntity? = locations[id]
        override suspend fun getSensorDataBySyncAndProcessedStatus(
            syncstatus: Boolean,
            procStatus: Boolean
        ): List<LocationEntity> = emptyList()

        override fun getLocationBySynced(syncStat: Boolean): Flow<List<LocationEntity>> = flowOf(emptyList())
        override suspend fun updateLocation(location: LocationEntity) = Unit
        override suspend fun updateLocations(locations: List<LocationEntity>) = Unit
        override suspend fun deleteAllLocations() = Unit
        override suspend fun deleteLocationsByIds(ids: List<UUID>) = Unit
        override suspend fun getLocationDataByTripId(tripId: UUID): List<LocationData> = emptyList()
    }
}
