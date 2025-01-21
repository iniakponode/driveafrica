package com.uoa.core.database.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.uoa.core.Sdadb
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
                    val flow = bufferCopy.asFlow().map { it.toEntity() }
                    unsafeDrivingAnalyser.analyze(flow, context).collect { unsafeEntity ->
                        Log.d(TAG, "Detected unsafe behavior: $unsafeEntity")
                        unsafeBehaviourDao.insertUnsafeBehaviour(unsafeEntity.toEntity())
                    }

                    // 3) AI Model Processing and Mark processed
                    Log.d(TAG, "Starting AI model processing for each record...")
                    bufferCopy.forEach { rawData ->
                        val locationId = rawData.locationId
                        if (locationId == null) {
                            Log.w(TAG, "Skipping rawData ID=${rawData.id}: locationId is null.")
                            return@forEach
                        }

                        val locationEntity = locationDao.getLocationById(locationId)
                        if (locationEntity == null) {
                            Log.w(TAG, "Skipping rawData ID=${rawData.id}: location $locationId not found.")
                            return@forEach
                        }

                        try {
                            Log.d(TAG, "Processing AI model for rawData ID=${rawData.id}, location=$locationId.")
                            aiModelInputRepository.processDataForAIModelInputs(
                                sensorData = rawData,
                                location = locationEntity.toDomainModel(),
                                tripId = rawData.tripId!!
                            )

                            Log.d(TAG, "Marking rawData ID=${rawData.id} and location=$locationId as processed.")
                            rawSensorDataDao.updateRawSensorData(
                                rawData.copy(processed = true).toEntity()
                            )
                            locationDao.updateLocation(locationEntity.copy(processed = true))
                        } catch (aiEx: Exception) {
                            Log.e(TAG, "AI processing failed for rawData ID=${rawData.id}, skipping. ${aiEx.message}", aiEx)
                            // Optional: decide if you want to throw to rollback or continue
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