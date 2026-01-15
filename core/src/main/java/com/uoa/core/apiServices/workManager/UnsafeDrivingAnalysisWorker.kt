package com.uoa.core.apiServices.workManager


import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.room.withTransaction
//import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.uoa.core.behaviouranalysis.NewUnsafeDrivingBehaviourAnalyser
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.Sdadb
import com.uoa.core.utils.toDomainModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.lang.Exception

/**
 * A Worker that periodically fetches batches of *unprocessed* RawSensorData from the local
 * database, analyzes them to detect unsafe driving behaviors, stores those behaviors in
 * the database, and marks the data as processed.
 */
@HiltWorker
class UnsafeDrivingAnalysisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val rawSensorDataRepository: RawSensorDataRepository,
    private val unsafeBehaviourRepository: UnsafeBehaviourRepository,
    private val unsafeDrivingAnalyser: NewUnsafeDrivingBehaviourAnalyser,
    private val locationRepository: LocationRepository,
    private val aiModelInputRepository: AIModelInputRepository,
    private val appDatabase: Sdadb
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val BATCH_SIZE = 3000
        private const val TAG = "UnsafeDrivingAnalysisWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting UnsafeDrivingAnalysisWorker...")

        try {
            // 1) Fetch unprocessed, unsynced raw sensor data
            val unprocessedData = rawSensorDataRepository.getRawSensorDataBySyncAndProcessedStatus(
                synced = false,
                processed = false
            )
            Log.d(TAG, "Fetched ${unprocessedData.size} unprocessed sensor data records.")

            if (unprocessedData.isEmpty()) {
                Log.d(TAG, "No unprocessed data found. Worker will return success.")
                return@withContext Result.success()
            }

            // 2) Break the data into chunks
            val dataChunks = unprocessedData.chunked(BATCH_SIZE)
            Log.d(TAG, "Split data into ${dataChunks.size} chunk(s). Batch size = $BATCH_SIZE")

            // 3) Process each chunk inside a DB transaction
            dataChunks.forEachIndexed { chunkIndex, chunk ->
                Log.d(
                    TAG,
                    "Processing chunk ${chunkIndex + 1} of ${dataChunks.size}, size: ${chunk.size}."
                )

                // Use a transaction so if any step fails, we roll back the entire chunk
                appDatabase.withTransaction {
                    // a) Filter out any raw data whose locationId or tripId is null
                    val invalidLocationItems = chunk.filter { it.locationId == null }
                    if (invalidLocationItems.isNotEmpty()) {
                        Log.w(TAG, "Skipping ${invalidLocationItems.size} items in this chunk because locationId is null.")
                        invalidLocationItems.forEach { item ->
                            rawSensorDataRepository.updateRawSensorData(item.copy(processed = true))
                        }
                    }

                    val invalidTripItems = chunk.filter { it.locationId != null && it.tripId == null }
                    if (invalidTripItems.isNotEmpty()) {
                        Log.w(TAG, "Skipping ${invalidTripItems.size} items in this chunk because tripId is null.")
                        invalidTripItems.forEach { item ->
                            rawSensorDataRepository.updateRawSensorData(item.copy(processed = true))
                        }
                    }

                    val validItems = chunk.filter { it.locationId != null && it.tripId != null }

                    // b) Convert valid items to a Flow and analyze unsafe behaviors
                    val sensorDataFlow = validItems.asFlow().map { it }
                    unsafeDrivingAnalyser.analyze(sensorDataFlow, applicationContext).collect { unsafeBehaviour ->
                        Log.d(TAG, "Unsafe behaviour detected: ${unsafeBehaviour.behaviorType}")
                        unsafeBehaviourRepository.insertUnsafeBehaviour(unsafeBehaviour)
                    }

                    // c) Process AI Model input for each valid item
                    validItems.forEachIndexed { index, rawSensorData ->
                        val locationId = rawSensorData.locationId
                        val tripId = rawSensorData.tripId

                        // Location ID should not be null now, but we double-check
                        if (locationId == null) {
                            Log.w(TAG, "Skipping item index=$index; locationId is null.")
                            rawSensorDataRepository.updateRawSensorData(rawSensorData.copy(processed = true))
                            return@forEachIndexed
                        }

                        // Fetch the location from DB
                        val locationEntity = locationRepository.getLocationById(locationId)
                        if (locationEntity == null) {
                            Log.w(TAG, "Skipping rawSensorData ID=${rawSensorData.id}; location not found for ID=$locationId.")
                            rawSensorDataRepository.updateRawSensorData(rawSensorData.copy(processed = true))
                            return@forEachIndexed
                        }

                        try {
                            if (tripId == null) {
                                Log.w(TAG, "Skipping rawSensorData ID=${rawSensorData.id}; tripId is null.")
                                rawSensorDataRepository.updateRawSensorData(rawSensorData.copy(processed = true))
                                return@forEachIndexed
                            }
                            // Actually run the AI model input processing
                            Log.d(TAG, "Processing AIModelInput for rawSensorData ID=${rawSensorData.id}, location ID=$locationId.")
                            aiModelInputRepository.processDataForAIModelInputs(
                                sensorData = rawSensorData.toDomainModel(),
                                location = locationEntity.toDomainModel(),
                                tripId = tripId
                            )

                            // Mark both rawSensorData & location as processed
                            val updatedRaw = rawSensorData.copy(processed = true)
                            rawSensorDataRepository.updateRawSensorData(updatedRaw)

                            val updatedLocation = locationEntity.copy(processed = true)
                            locationRepository.updateLocation(updatedLocation)

                            Log.d(TAG, "Marked rawSensorData ID=${rawSensorData.id} and location ID=$locationId as processed.")

                        } catch (e: Exception) {
                            // Throw the exception so we roll back the transaction for this entire chunk
                            Log.e(TAG, "Error processing AIModelInput for rawSensorData ID=${rawSensorData.id}: ${e.message}", e)
                            throw e
                        }
                    }
                }

                Log.d(TAG, "Finished processing chunk ${chunkIndex + 1} of ${dataChunks.size}.")
            }

            Log.d(TAG, "All chunks processed successfully. Worker returns success.")
            Result.success()

        } catch (e: Exception) {
            // If anything fails in any transaction, we retry
            Log.e(TAG, "Worker encountered an error: ${e.message}. Retrying...", e)
            Result.retry()
        }
    }
}
