package com.uoa.core.apiServices.workManager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.uoa.core.apiServices.services.rawSensorApiService.RawSensorDataApiRepository
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.ReportStatisticsRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.notifications.VehicleNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID
@HiltWorker
class DeleteLocalDataWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val localRawDataRepository: RawSensorDataRepository,
    private val locationLocalRepository: LocationRepository,
    private val unsafeBehavioursLocalRepository: UnsafeBehaviourRepository,
    private val aiModelInputLocalRepository: AIModelInputRepository,
    private val reportStatisticsRepository: ReportStatisticsRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val vehicleNotificationManager = VehicleNotificationManager(applicationContext)

        // Helper function to delete data if list is not empty
        suspend fun <T> deleteIfNotEmpty(
            items: List<T>,
            deleteAction: suspend (List<T>) -> Unit
        ) {
            if (items.isNotEmpty()) {
                deleteAction(items)
            }
        }

        // 1) Pull data in an order that ensures child records are deleted first
        //    so foreign key constraints won't fail (e.g., if rawSensorData references location).
        val syncedAndProcessedBehaviours = unsafeBehavioursLocalRepository.getUnsafeBehaviourBySyncAndProcessedStatus(true, true)
        val syncedAndProcessedRawData = localRawDataRepository.getRawSensorDataBySyncAndProcessedStatus(true, true)
        val syncedAndProcessedLocations = locationLocalRepository.getSensorDataBySyncAndProcessedStatus(true, true)
        val syncedAndProcessedAIModelInputs = aiModelInputLocalRepository.getAiModelInputsBySyncAndProcessedStatus(true, true)
        val syncedAndProcessedReportStatistics = reportStatisticsRepository.getReportStatisticsBySyncAndProcessedStatus(true, true)

        // 2) If all are empty, nothing to delete
        if (
            syncedAndProcessedRawData.isEmpty() &&
            syncedAndProcessedLocations.isEmpty() &&
            syncedAndProcessedBehaviours.isEmpty() &&
            syncedAndProcessedAIModelInputs.isEmpty() &&
            syncedAndProcessedReportStatistics.isEmpty()
        ) {
            return Result.success()
        }

        // 3) Notify user that cleanup is starting
        vehicleNotificationManager.displayNotification("Data Cleanup", "Deleting locally processed data...")

        // 4) Delete in an order that respects foreign key constraints:
        //    - If X references Y, delete X before Y.

        // Example: If unsafeBehaviours references location or raw data, delete it first:
        deleteIfNotEmpty(
            syncedAndProcessedBehaviours
        ) { data ->
            val behaviourIdsToDelete = data.map { it.id }
            unsafeBehavioursLocalRepository.deleteUnsafeBehavioursByIds(behaviourIdsToDelete)
        }

        // Next, if rawSensorData references location, delete rawSensorData before location
        deleteIfNotEmpty(
            syncedAndProcessedRawData
        ) { data ->
            val rawDataIdsToDelete = data.map { it.id }
            localRawDataRepository.deleteRawSensorDataByIds(rawDataIdsToDelete)
        }

        // Then delete location
        deleteIfNotEmpty(
            syncedAndProcessedLocations
        ) { data ->
            val locationIdsToDelete = data.map { it.id }
            locationLocalRepository.deleteLocationsByIds(locationIdsToDelete)
        }

        // Delete AIModelInput
        deleteIfNotEmpty(
            syncedAndProcessedAIModelInputs
        ) { data ->
            val aiModelInputIdsToDelete = data.map { it.id }
            aiModelInputLocalRepository.deleteAIModelInputsByIds(aiModelInputIdsToDelete)
        }

        // Finally, delete report statistics
        deleteIfNotEmpty(
            syncedAndProcessedReportStatistics
        ) { data ->
            val reportStatisticsIdsToDelete = data.map { it.id }
            reportStatisticsRepository.deleteReportStatisticsByIds(reportStatisticsIdsToDelete)
        }

        // 5) Notify that cleanup is finished
        vehicleNotificationManager.displayNotification("Data Cleanup", "Local data cleanup completed.")
        return Result.success()
    }
}