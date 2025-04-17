package com.uoa.core.database.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.uoa.core.Sdadb
import com.uoa.core.apiServices.workManager.UnsafeDrivingAnalysisWorker
import com.uoa.core.behaviouranalysis.NewUnsafeDrivingBehaviourAnalyser
import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.daos.RawSensorDataDao
import com.uoa.core.database.daos.UnsafeBehaviourDao
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessAndStoreSensorData @Inject constructor(
    private val rawSensorDataDao: RawSensorDataDao,
    private val appDatabase: Sdadb,
    private val context: Context,
    private val unsafeDrivingAnalyser: NewUnsafeDrivingBehaviourAnalyser,
    private val aiModelInputRepository: AIModelInputRepository,
    private val locationDao: LocationDao,
    private val unsafeBehaviourDao: UnsafeBehaviourDao,
) {
    suspend fun processAndStoreSensorData(bufferCopy: List<RawSensorData>) {
        val TAG = "SENSORDATAPROCESSING"
        withContext(Dispatchers.IO) {
            try {
                appDatabase.withTransaction {
                    // 1) Insert raw data in bulk
                    Log.d(TAG, "Inserting raw sensor data batch, count=${bufferCopy.size}.")
                    rawSensorDataDao.insertRawSensorDataBatch(bufferCopy.map { it.toEntity() })

                    // 2) Analyze data for unsafe behaviors
                    Log.d(TAG, "Analyzing data for unsafe behaviors...")

                    // a) Filter out any raw data whose locationId is null
                    val validItems = bufferCopy.filter { it.locationId != null }
                    val invalidItems = bufferCopy.size - validItems.size
                    if (invalidItems > 0) {
                        Log.w(TAG, "Skipping $invalidItems items in this buffer copy because locationId is null.")
                    }

                    // b) Convert valid items to a Flow and analyze unsafe behaviors
                    val sensorDataFlow = validItems.asFlow().map { it.toEntity() }
                    unsafeDrivingAnalyser.analyze(sensorDataFlow, context).collect { unsafeBehaviour ->
                        Log.d(TAG, "Unsafe behaviour detected: ${unsafeBehaviour.behaviorType}")
                        unsafeBehaviourDao.insertUnsafeBehaviour(unsafeBehaviour.toEntity())
                    }

                    // 3) AI Model Processing and Mark processed
                    Log.d(TAG, "Starting AI model processing for each record...")
                    validItems.forEachIndexed { index, rawSensorData ->
                        val locationId = rawSensorData.locationId
                        val tripId = rawSensorData.tripId

                        // Location ID should not be null now, but we double-check
                        if (locationId == null) {
                            Log.w(TAG, "Skipping item index=$index; locationId is null.")
                            return@forEachIndexed
                        }

                        // Fetch the location from DB
                        val locationEntity = locationDao.getLocationById(locationId)
                        if (locationEntity == null) {
                            Log.w(TAG, "Skipping rawSensorData ID=${rawSensorData.id}; location not found for ID=$locationId.")
                            return@forEachIndexed
                        }

                        try {
                            // Actually run the AI model input processing
                            Log.d(TAG, "Processing AIModelInput for rawSensorData ID=${rawSensorData.id}, location ID=$locationId.")
                            aiModelInputRepository.processDataForAIModelInputs(
                                sensorData = rawSensorData,
                                location = locationEntity.toDomainModel(),
                                tripId = tripId!!
                            )

                            // Mark both rawSensorData & location as processed
                            val updatedRaw = rawSensorData.copy(processed = true)
                            rawSensorDataDao.updateRawSensorData(updatedRaw.toEntity())

                            val updatedLocation = locationEntity.copy(processed = true)
                            locationDao.updateLocation(updatedLocation)

                            Log.d(TAG, "Marked rawSensorData ID=${rawSensorData.id} and location ID=$locationId as processed.")

                        } catch (e: java.lang.Exception) {
                            // Throw the exception so we roll back the transaction for this entire chunk
                            Log.e(TAG, "Error processing AIModelInput for rawSensorData ID=${rawSensorData.id}: ${e.message}", e)
                            throw e
                        }
                    }

                    Log.d(TAG, "Completed processAndStoreSensorData transaction successfully.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in processAndStoreSensorData transaction, rolling back: ${e.message}", e)
                throw e
            }
        }
    }

}