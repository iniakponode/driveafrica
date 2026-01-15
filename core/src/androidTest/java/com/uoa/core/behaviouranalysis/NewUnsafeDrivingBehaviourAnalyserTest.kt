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
    fun harshAccelerationDetectedWithLinearAcceleration() = runBlocking {
        val locationId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val driverId = UUID.randomUUID()
        val locationEntity = LocationEntity(
            id = locationId,
            latitude = 0.0,
            longitude = 0.0,
            timestamp = 3_000L,
            date = Date(3_000L),
            altitude = 0.0,
            speed = 10f,
            distance = 0f,
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
            timestamp = 3_000L,
            date = Date(3_000L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )
        val dataStart = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.LINEAR_ACCELERATION_TYPE,
            sensorTypeName = "Linear Acceleration",
            values = listOf(4.2f, 0f, 0f),
            timestamp = 3_100L,
            date = Date(3_100L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )
        val dataFollowUp = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.LINEAR_ACCELERATION_TYPE,
            sensorTypeName = "Linear Acceleration",
            values = listOf(4.2f, 0f, 0f),
            timestamp = 3_450L,
            date = Date(3_450L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )

        val results = analyser.analyze(flowOf(seed, dataStart, dataFollowUp), context).toList()

        assertEquals(1, results.size)
        val behavior = results.first()
        assertEquals("Harsh Acceleration", behavior.behaviorType)
        assertEquals(tripId, behavior.tripId)
        assertEquals(locationId, behavior.locationId)
    }

    @Test
    fun harshBrakingDetectedWithLinearAcceleration() = runBlocking {
        val locationId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val driverId = UUID.randomUUID()
        val locationEntity = LocationEntity(
            id = locationId,
            latitude = 0.0,
            longitude = 0.0,
            timestamp = 4_000L,
            date = Date(4_000L),
            altitude = 0.0,
            speed = 10f,
            distance = 0f,
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
            timestamp = 4_000L,
            date = Date(4_000L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )
        val dataStart = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.LINEAR_ACCELERATION_TYPE,
            sensorTypeName = "Linear Acceleration",
            values = listOf(-4.8f, 0f, 0f),
            timestamp = 4_100L,
            date = Date(4_100L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )
        val dataFollowUp = RawSensorDataEntity(
            id = UUID.randomUUID(),
            sensorType = NewUnsafeDrivingBehaviourAnalyser.LINEAR_ACCELERATION_TYPE,
            sensorTypeName = "Linear Acceleration",
            values = listOf(-4.8f, 0f, 0f),
            timestamp = 4_450L,
            date = Date(4_450L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )

        val results = analyser.analyze(flowOf(seed, dataStart, dataFollowUp), context).toList()

        assertEquals(1, results.size)
        val behavior = results.first()
        assertEquals("Harsh Braking", behavior.behaviorType)
        assertEquals(tripId, behavior.tripId)
        assertEquals(locationId, behavior.locationId)
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
            speed = 10f,
            distance = 0f,
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
            sensorType = NewUnsafeDrivingBehaviourAnalyser.ROTATION_VECTOR_TYPE,
            sensorTypeName = "Rotation Vector",
            values = listOf(0.3f, 0f, 0f),
            timestamp = 5_000L,
            date = Date(5_000L),
            accuracy = 3,
            locationId = locationId,
            tripId = tripId,
            driverProfileId = driverId,
            processed = false,
            sync = false
        )

        val results = analyser.analyze(flowOf(seed, data), context).toList()

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
