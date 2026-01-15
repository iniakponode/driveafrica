package com.uoa.sensor.repository

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.uoa.core.model.Road
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

class SensorDataColStateRepositoryTest {

    @Test
    fun updateRoadContextUpdatesRoadsAndSpeedLimit() = runTest {
        val drivingStateStore = mock<DrivingStateStore>()
        whenever(drivingStateStore.snapshotFlow).thenReturn(emptyFlow())

        val repository = SensorDataColStateRepository(drivingStateStore)
        val road = Road(
            id = UUID.randomUUID(),
            driverProfileId = UUID.randomUUID(),
            name = "Test Road",
            roadType = "residential",
            speedLimit = 45,
            latitude = 0.0,
            longitude = 0.0,
            radius = 0.0,
            sync = false
        )

        repository.updateRoadContext(listOf(road), 45)

        assertEquals(listOf(road), repository.nearbyRoads.value)
        assertEquals(45, repository.speedLimit.value)
        assertEquals(0.0, repository.distanceTravelled.value)
        assertEquals(emptyList<org.osmdroid.util.GeoPoint>(), repository.pathPoints.value)
    }
}
