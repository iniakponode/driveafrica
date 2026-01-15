package com.uoa.core.apiServices.workManager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.model.SyncState
import com.uoa.core.model.Trip
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class DeleteLocalDataWorkerTest {

    @Test
    fun deletesEligibleTripsAndArchives() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val localRawDataRepository = mock<RawSensorDataRepository>()
        val rawSensorDataRepository = mock<RawSensorDataRepository>()
        val locationLocalRepository = mock<LocationRepository>()
        val unsafeBehavioursLocalRepository = mock<UnsafeBehaviourRepository>()
        val aiModelInputLocalRepository = mock<AIModelInputRepository>()
        val tripRepository = mock<TripDataRepository>()

        val summaryTrip = Trip(
            id = UUID.randomUUID(),
            driverPId = null,
            startTime = 0L,
            endTime = 1L,
            startDate = null,
            endDate = null,
            influence = null,
            sync = false,
            syncState = SyncState.SUMMARY_READY
        )
        val rawUploadedTrip = Trip(
            id = UUID.randomUUID(),
            driverPId = null,
            startTime = 0L,
            endTime = 1L,
            startDate = null,
            endDate = null,
            influence = null,
            sync = false,
            syncState = SyncState.RAW_DATA_UPLOADED
        )

        whenever(tripRepository.getTripsBySyncState(SyncState.SUMMARY_READY))
            .thenReturn(listOf(summaryTrip))
        whenever(tripRepository.getTripsBySyncState(SyncState.RAW_DATA_UPLOADED))
            .thenReturn(listOf(rawUploadedTrip))
        whenever(rawSensorDataRepository.countUnsyncedRawSensorDataByTripId(any()))
            .thenReturn(0)

        val retainedLocation = UUID.randomUUID()
        whenever(unsafeBehavioursLocalRepository.getLocationIdsWithUnsafeBehavioursSince(any()))
            .thenReturn(listOf(retainedLocation))

        val summaryLocations = listOf(UUID.randomUUID(), retainedLocation)
        val rawLocations = listOf(UUID.randomUUID())
        whenever(rawSensorDataRepository.getLocationIdsByTripId(summaryTrip.id))
            .thenReturn(summaryLocations)
        whenever(rawSensorDataRepository.getLocationIdsByTripId(rawUploadedTrip.id))
            .thenReturn(rawLocations)

        val worker = buildDeleteWorker(
            context,
            localRawDataRepository,
            locationLocalRepository,
            unsafeBehavioursLocalRepository,
            aiModelInputLocalRepository,
            rawSensorDataRepository,
            tripRepository
        )

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        verify(unsafeBehavioursLocalRepository).deleteUnsafeBehavioursBefore(any())
        verify(tripRepository).updateSyncState(summaryTrip.id, SyncState.RAW_DATA_UPLOADED)
        verify(tripRepository).updateSyncState(summaryTrip.id, SyncState.ARCHIVED)
        verify(tripRepository).updateSyncState(rawUploadedTrip.id, SyncState.ARCHIVED)
        verify(aiModelInputLocalRepository).deleteAiModelInputsByTripId(summaryTrip.id)
        verify(aiModelInputLocalRepository).deleteAiModelInputsByTripId(rawUploadedTrip.id)
        verify(localRawDataRepository).deleteRawSensorDataByTripId(summaryTrip.id)
        verify(localRawDataRepository).deleteRawSensorDataByTripId(rawUploadedTrip.id)

        val captor = argumentCaptor<List<UUID>>()
        verify(locationLocalRepository, times(2)).deleteLocationsByIds(captor.capture())
        assertTrue(captor.allValues.any { it == listOf(summaryLocations.first()) })
        assertTrue(captor.allValues.any { it == rawLocations })
    }

    @Test
    fun skipsTripsWithUnsyncedRawData() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val localRawDataRepository = mock<RawSensorDataRepository>()
        val rawSensorDataRepository = mock<RawSensorDataRepository>()
        val locationLocalRepository = mock<LocationRepository>()
        val unsafeBehavioursLocalRepository = mock<UnsafeBehaviourRepository>()
        val aiModelInputLocalRepository = mock<AIModelInputRepository>()
        val tripRepository = mock<TripDataRepository>()

        val summaryTrip = Trip(
            id = UUID.randomUUID(),
            driverPId = null,
            startTime = 0L,
            endTime = 1L,
            startDate = null,
            endDate = null,
            influence = null,
            sync = false,
            syncState = SyncState.SUMMARY_READY
        )

        whenever(tripRepository.getTripsBySyncState(SyncState.SUMMARY_READY))
            .thenReturn(listOf(summaryTrip))
        whenever(tripRepository.getTripsBySyncState(SyncState.RAW_DATA_UPLOADED))
            .thenReturn(emptyList())
        whenever(rawSensorDataRepository.countUnsyncedRawSensorDataByTripId(summaryTrip.id))
            .thenReturn(2)

        val worker = buildDeleteWorker(
            context,
            localRawDataRepository,
            locationLocalRepository,
            unsafeBehavioursLocalRepository,
            aiModelInputLocalRepository,
            rawSensorDataRepository,
            tripRepository
        )

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        verify(unsafeBehavioursLocalRepository).deleteUnsafeBehavioursBefore(any())
        verify(aiModelInputLocalRepository, never()).deleteAiModelInputsByTripId(any())
        verify(localRawDataRepository, never()).deleteRawSensorDataByTripId(any())
        verify(locationLocalRepository, never()).deleteLocationsByIds(any())
        verify(tripRepository, never()).updateSyncState(any(), any())
    }

    private fun buildDeleteWorker(
        context: Context,
        localRawDataRepository: RawSensorDataRepository,
        locationLocalRepository: LocationRepository,
        unsafeBehavioursLocalRepository: UnsafeBehaviourRepository,
        aiModelInputLocalRepository: AIModelInputRepository,
        rawSensorDataRepository: RawSensorDataRepository,
        tripRepository: TripDataRepository
    ): DeleteLocalDataWorker {
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker? {
                return DeleteLocalDataWorker(
                    appContext,
                    workerParameters,
                    localRawDataRepository,
                    locationLocalRepository,
                    unsafeBehavioursLocalRepository,
                    aiModelInputLocalRepository,
                    rawSensorDataRepository,
                    tripRepository
                )
            }
        }

        return TestListenableWorkerBuilder<DeleteLocalDataWorker>(context)
            .setWorkerFactory(factory)
            .build()
    }
}
