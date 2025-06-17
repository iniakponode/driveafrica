package com.uoa.sensor.repository

import android.content.Context
import android.util.Log
import com.uoa.core.database.daos.RawSensorDataDao
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.toDomainModel


import com.uoa.core.utils.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import com.uoa.core.Sdadb
import androidx.room.Transaction
import com.uoa.core.behaviouranalysis.NewUnsafeDrivingBehaviourAnalyser
import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.daos.UnsafeBehaviourDao
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.database.repository.ProcessAndStoreSensorData
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class RawSensorDataRepositoryImpl @Inject constructor(
    private val rawSensorDataDao: RawSensorDataDao,
    private val processAndStoreSensorData: ProcessAndStoreSensorData
):
    RawSensorDataRepository {

    override fun getRawSensorDataBetween(start: LocalDate, end: LocalDate): Flow<List<RawSensorData>> {
        val rawSensorDataFlow=rawSensorDataDao.getRawSensorDataBetween(start, end)
        return rawSensorDataFlow.map { rawSensorDataList ->
            rawSensorDataList.map { it.toDomainModel() }
        }
    }

    override fun getRawSensorDataByTripId(tripId: UUID): Flow<List<RawSensorDataEntity>> {
        return rawSensorDataDao.getRawSensorDataByTripId(tripId)
    }

    override fun getRawSensorDataByLocationId(locationId: UUID): Flow<List<RawSensorDataEntity>> {
        return rawSensorDataDao.getRawSensorDataByLocationId(locationId)
    }
    override suspend fun insertRawSensorData(rawSensorData: RawSensorData) {
        withContext(Dispatchers.IO) {
            rawSensorDataDao.insertRawSensorData(rawSensorData.toEntity())
        }
    }

    override suspend fun getAllRawSensorDataInChunks(
        tripId: UUID,
        chunkSize: Int
    ): Flow<List<RawSensorDataEntity>> = flow {
        var lastId: UUID? = null
        while (true) {
            val chunk = rawSensorDataDao.getRawSensorDataChunkAfterId(tripId, chunkSize, lastId)
            if (chunk.isEmpty()) {
                break // No more data to fetch
            }
            emit(chunk)
            lastId = chunk.last().id
        }
    }

    override suspend fun insertRawSensorDataBatch(rawSensorDataList: List<RawSensorDataEntity>) {
        withContext(Dispatchers.IO) {
            rawSensorDataDao.insertRawSensorDataBatch(rawSensorDataList)
        }
    }

    override suspend fun getRawSensorDataById(id: UUID): RawSensorDataEntity? {
        return rawSensorDataDao.getRawSensorDataById(id)
    }

    override suspend fun getSensorDataByLocationIdAndSyncStatus(
        locationId: UUID,
        synced: Boolean,
        processed: Boolean
    ): List<RawSensorDataEntity> {
        return rawSensorDataDao.getSensorDataByLocationIdAndSyncStatus(locationId,synced, processed)
    }

    override fun getUnsyncedRawSensorData(): Flow<List<RawSensorDataEntity>> {
        return rawSensorDataDao.getUnsyncedRawSensorData()
    }

    override suspend fun getRawSensorDataBySyncAndProcessedStatus(
        synced: Boolean,
        processed: Boolean
    ): List<RawSensorDataEntity> {
        return rawSensorDataDao.getSensorDataBySyncAndProcessedStatus(synced,processed)
    }

    override suspend fun getSensorDataBySyncStatus(synced: Boolean): List<RawSensorData> {
        return rawSensorDataDao.getSensorDataBySyncStatus(synced).map { it.toDomainModel() }
        }

    override suspend fun updateRawSensorData(rawSensorData: RawSensorDataEntity) {
        rawSensorDataDao.updateRawSensorData(rawSensorData)
    }

    override suspend fun deleteAllRawSensorData() {
        rawSensorDataDao.deleteAllRawSensorData()
    }

    override suspend fun deleteRawSensorDataByIds(ids: List<UUID>) {
        rawSensorDataDao.deleteRawSensorDataByIds(ids)
    }

    override suspend fun getSensorDataBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<RawSensorDataEntity>> {
        return withContext(Dispatchers.IO){

            rawSensorDataDao.getSensorDataBetweenDates(startDate, endDate)
        }

    }

    override suspend fun getSensorDataByTripId(tripId: UUID): Flow<List<RawSensorDataEntity>> {
        return withContext(Dispatchers.IO){
            rawSensorDataDao.getSensorDataByTripId(tripId)
        }
    }

    /**
     * A concrete repository implementation that uses Room.
     * The @Transaction is done using either `withTransaction` or a DAO-based approach.
     */
    override suspend fun processAndStoreSensorData(bufferCopy: List<RawSensorData>) {
        processAndStoreSensorData.processAndStoreSensorData(bufferCopy)
    }


}
