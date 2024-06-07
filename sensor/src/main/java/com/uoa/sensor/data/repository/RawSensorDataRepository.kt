package com.uoa.sensor.data.repository

import com.uoa.core.database.daos.RawSensorDataDao
import com.uoa.core.database.entities.RawSensorDataEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class RawSensorDataRepository(private val rawSensorDataDao: RawSensorDataDao) {

    fun getRawSensorDataBetween(start: Instant, end: Instant): Flow<List<RawSensorDataEntity>> {
        return rawSensorDataDao.getRawSensorDataBetween(start, end)
    }

    suspend fun insertRawSensorData(rawSensorData: RawSensorDataEntity) {
        rawSensorDataDao.insertRawSensorData(rawSensorData)
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
