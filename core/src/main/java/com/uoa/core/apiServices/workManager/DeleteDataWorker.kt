package com.uoa.core.apiServices.workManager

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.model.SyncState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
@HiltWorker
class DeleteLocalDataWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val localRawDataRepository: RawSensorDataRepository,
    private val locationLocalRepository: LocationRepository,
    private val unsafeBehavioursLocalRepository: UnsafeBehaviourRepository,
    private val aiModelInputLocalRepository: AIModelInputRepository,
    private val rawSensorDataRepository: RawSensorDataRepository,
    private val tripRepository: TripDataRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val retentionMs = TimeUnit.DAYS.toMillis(14)
        val cutoffTimestamp = System.currentTimeMillis() - retentionMs

        // Keep unsafe behaviours for 14 days.
        unsafeBehavioursLocalRepository.deleteUnsafeBehavioursBefore(cutoffTimestamp)

        val summaryReadyTrips = tripRepository.getTripsBySyncState(SyncState.SUMMARY_READY)
        val rawUploadedTrips = tripRepository.getTripsBySyncState(SyncState.RAW_DATA_UPLOADED)
        val eligibleTrips = (summaryReadyTrips + rawUploadedTrips).distinctBy { it.id }

        if (eligibleTrips.isEmpty()) {
            return Result.success()
        }

        val retainedLocationIds = unsafeBehavioursLocalRepository
            .getLocationIdsWithUnsafeBehavioursSince(cutoffTimestamp)
            .orEmpty()
            .toSet()

        eligibleTrips.forEach { trip ->
            val tripTimestamp = trip.endTime ?: trip.startTime
            if (tripTimestamp > cutoffTimestamp) {
                return@forEach
            }

            val unsyncedCount = rawSensorDataRepository.countUnsyncedRawSensorDataByTripId(trip.id)
            if (unsyncedCount > 0) {
                return@forEach
            }

            if (trip.syncState == SyncState.SUMMARY_READY) {
                tripRepository.updateSyncState(trip.id, SyncState.RAW_DATA_UPLOADED)
            }

            val locationIds = rawSensorDataRepository.getLocationIdsByTripId(trip.id)
            val deletableLocations = locationIds.filterNot { retainedLocationIds.contains(it) }

            aiModelInputLocalRepository.deleteAiModelInputsByTripId(trip.id)
            localRawDataRepository.deleteRawSensorDataByTripId(trip.id)

            if (deletableLocations.isNotEmpty()) {
                locationLocalRepository.deleteLocationsByIds(deletableLocations)
            }

            tripRepository.updateSyncState(trip.id, SyncState.ARCHIVED)
        }

        return Result.success()
    }
}
