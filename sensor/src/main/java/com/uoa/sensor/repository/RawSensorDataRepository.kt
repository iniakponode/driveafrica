package com.uoa.sensor.data.repository

import com.uoa.core.database.daos.RawSensorDataDao
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant

class RawSensorDataRepository(private val rawSensorDataDao: RawSensorDataDao) {

    fun getRawSensorDataBetween(start: Instant, end: Instant): Flow<List<RawSensorData>> {
        val rawSensorDataFlow=rawSensorDataDao.getRawSensorDataBetween(start, end)
        return rawSensorDataFlow.map { rawSensorDataList ->
            rawSensorDataList.map { it.toDomainModel() }
        }
    }

    fun getRawSensorDataByTripId(tripId: Int): Flow<List<RawSensorDataEntity>> {
        return rawSensorDataDao.getRawSensorDataByTripId(tripId)
    }

    fun getRawSensorDataByLocationId(locationId: Int): Flow<List<RawSensorDataEntity>> {
        return rawSensorDataDao.getRawSensorDataByLocationId(locationId)
    }
    suspend fun insertRawSensorData(rawSensorData: RawSensorData) {
        withContext(Dispatchers.IO) {
            rawSensorDataDao.insertRawSensorData(rawSensorData.toEntity())
        }
    }

    suspend fun insertRawSensorDataBatch(rawSensorDataList: List<RawSensorDataEntity>) {
        withContext(Dispatchers.IO) {
            rawSensorDataDao.insertRawSensorDataBatch(rawSensorDataList)
        }
    }

    suspend fun getRawSensorDataById(id: Int): RawSensorDataEntity? {
        return rawSensorDataDao.getRawSensorDataById(id)
    }

    fun getUnsyncedRawSensorData(): Flow<List<RawSensorDataEntity>> {
        return rawSensorDataDao.getUnsyncedRawSensorData()
    }

    suspend fun updateRawSensorData(rawSensorData: RawSensorDataEntity) {
        rawSensorDataDao.updateRawSensorData(rawSensorData)
    }

    suspend fun deleteAllRawSensorData() {
        rawSensorDataDao.deleteAllRawSensorData()
    }
}
