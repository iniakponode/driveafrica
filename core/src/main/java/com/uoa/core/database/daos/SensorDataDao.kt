package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.SensorEntity

@Dao
interface SensorDataDao {
    @Insert
    suspend fun insertSensorData(sensorEntity: SensorEntity): Long

    @Update
    suspend fun updateSensorData(sensorEntity: SensorEntity)

    @Query("SELECT * FROM sensor_data")
    suspend fun getAllSensorData(): List<SensorEntity>

    @Query("SELECT * FROM sensor_data WHERE tripDataId = :tripDataId")
    suspend fun getSensorDataByTripId(tripDataId: Long): List<SensorEntity>

    @Query("SELECT * FROM sensor_data WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getSensorDataByTimePeriod(startTime: Long, endTime: Long): List<SensorEntity>

    @Query("SELECT * FROM sensor_data WHERE synced = :synced")
    suspend fun getSensorDataBySyncStatus(synced: Boolean): List<SensorEntity>

    @Query("UPDATE sensor_data SET synced=:synced WHERE id = :id")
    suspend fun updateUploadStatus(id: Long, synced: Boolean)

    @Query("DELETE FROM sensor_data WHERE id = :id")
    suspend fun deleteSensorDataById(id: Long)
}
