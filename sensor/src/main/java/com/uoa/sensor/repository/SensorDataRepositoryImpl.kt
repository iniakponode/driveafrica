package com.uoa.sensor.repository

import com.uoa.core.database.daos.SensorDataDao
import com.uoa.core.database.entities.SensorEntity
import com.uoa.core.model.SensorData
import org.modelmapper.ModelMapper
import javax.inject.Inject

class SensorDataRepositoryImpl @Inject constructor(private val sensorDataDao: SensorDataDao, private val modelMapper: ModelMapper) {

    suspend fun insertSensorData(sensorEntity: SensorEntity): Long {
        return sensorDataDao.insertSensorData(sensorEntity)
    }

    suspend fun updateSensorData(sensorEntity: SensorEntity) {
        sensorDataDao.updateSensorData(sensorEntity)
    }

    suspend fun getAllSensorData(): List<SensorData> {
        val entities= sensorDataDao.getAllSensorData()
        return entities.map { sensorEntity -> modelMapper.map(sensorEntity, SensorData::class.java) }
    }

    suspend fun updateUploadStatus(id: Long, sync: Boolean) {
        return sensorDataDao.updateUploadStatus(id, sync)
    }

    suspend fun getSensorDataByTripId(tripDataId: Long): List<SensorData> {
        val entities=sensorDataDao.getSensorDataByTripId(tripDataId)
        return entities.map { entity->modelMapper.map(entity, SensorData::class.java)}
    }

    suspend fun getSensorDataByTimePeriod(startTime: Long, endTime: Long): List<SensorData>{
        // Retrieve the list of SensorDataEntity objects from the database
        val entities = sensorDataDao.getSensorDataByTimePeriod(startTime, endTime)
        // Map each SensorDataEntity to SensorData using ModelMapper
        return entities.map { entity -> modelMapper.map(entity, SensorData::class.java) }
    }

    suspend fun getSensorDataBySyncStatus(synced: Boolean): List<SensorData> {
        val entities=sensorDataDao.getSensorDataBySyncStatus(synced)
        return entities.map { entity->modelMapper.map(entity, SensorData::class.java) }
    }

    suspend fun deleteSensorDataById(id: Long) {
        sensorDataDao.deleteSensorDataById(id)
    }
}
