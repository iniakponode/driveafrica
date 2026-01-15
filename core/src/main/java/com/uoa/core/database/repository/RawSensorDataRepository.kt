package com.uoa.core.database.repository

import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.entities.UnsafeBehaviourEntity
import com.uoa.core.model.RawSensorData
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

interface RawSensorDataRepository {
fun getRawSensorDataBetween(start: LocalDate, end: LocalDate): Flow<List<RawSensorData>>
fun getRawSensorDataByTripId(tripId: UUID): Flow<List<RawSensorDataEntity>>
suspend fun getSensorDataByTripId(tripId: UUID): Flow<List<RawSensorDataEntity>>
fun getRawSensorDataByLocationId(locationId: UUID): Flow<List<RawSensorDataEntity>>
suspend fun getSensorDataBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<RawSensorDataEntity>>
suspend fun insertRawSensorData(rawSensorData: RawSensorData)
suspend fun insertRawSensorDataBatch(rawSensorDataList: List<RawSensorDataEntity>)
suspend fun getRawSensorDataById(id: UUID): RawSensorDataEntity?
suspend fun getSensorDataByLocationIdAndSyncStatus(locationId: UUID, synced: Boolean, processed: Boolean): List<RawSensorDataEntity>
fun getUnsyncedRawSensorData(): Flow<List<RawSensorDataEntity>>
suspend fun getRawSensorDataBySyncAndProcessedStatus(synced: Boolean, processed: Boolean): List<RawSensorDataEntity>
suspend fun getSensorDataBySyncStatus(synced: Boolean): List<RawSensorData>
suspend fun updateRawSensorData(rawSensorData: RawSensorDataEntity)
suspend fun deleteAllRawSensorData()
suspend fun deleteRawSensorDataByIds(ids: List<UUID>)
suspend fun deleteRawSensorDataByTripId(tripId: UUID)
suspend fun countUnsyncedRawSensorDataByTripId(tripId: UUID): Int
suspend fun getLocationIdsByTripId(tripId: UUID): List<UUID>
suspend fun getAllRawSensorDataInChunks(tripId: UUID,
                                        chunkSize: Int = 1000): Flow<List<RawSensorDataEntity>>
    /**
     * Atomic method that:
     * 1) Inserts the raw sensor data
     * 2) Analyzes for unsafe behavior
     * 3) Runs AI model logic
     * 4) Marks data (and related location) as processed
     *
     * Should run in a single transaction.
     */
    suspend fun processAndStoreSensorData(bufferCopy: List<RawSensorData>)
}
