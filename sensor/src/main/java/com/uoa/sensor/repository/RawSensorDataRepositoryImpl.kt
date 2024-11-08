package com.uoa.sensor.repository

import com.uoa.core.database.daos.RawSensorDataDao
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.repository.RawSensorDataRepository
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.toDomainModel

import com.uoa.core.utils.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class RawSensorDataRepositoryImpl(private val rawSensorDataDao: RawSensorDataDao):
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

    override suspend fun insertRawSensorDataBatch(rawSensorDataList: List<RawSensorDataEntity>) {
        withContext(Dispatchers.IO) {
            rawSensorDataDao.insertRawSensorDataBatch(rawSensorDataList)
        }
    }

    override suspend fun getRawSensorDataById(id: UUID): RawSensorDataEntity? {
        return rawSensorDataDao.getRawSensorDataById(id)
    }

    override fun getUnsyncedRawSensorData(): Flow<List<RawSensorDataEntity>> {
        return rawSensorDataDao.getUnsyncedRawSensorData()
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
}
