package com.uoa.core.database.daos
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.RawSensorDataEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
@Dao
interface RawSensorDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawSensorData(rawSensorData: RawSensorDataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawSensorDataBatch(rawSensorDataList: List<RawSensorDataEntity>)

    @Query("SELECT * FROM raw_sensor_data WHERE tripId = :tripId")
    fun getRawSensorDataByTripId(tripId: Int): Flow<List<RawSensorDataEntity>>

    @Query("SELECT * FROM raw_sensor_data WHERE locationId = :locationId")
    fun getRawSensorDataByLocationId(locationId: Int): Flow<List<RawSensorDataEntity>>
    @Query("SELECT * FROM raw_sensor_data WHERE timestamp >= :start AND timestamp <= :end")
    fun getRawSensorDataBetween(start: Instant, end: Instant): Flow<List<RawSensorDataEntity>>

    @Query("SELECT * FROM raw_sensor_data WHERE id = :id")
    suspend fun getRawSensorDataById(id: Int): RawSensorDataEntity?

    @Query("SELECT * FROM raw_sensor_data WHERE sync = 0")
    fun getUnsyncedRawSensorData(): Flow<List<RawSensorDataEntity>>

    @Update
    suspend fun updateRawSensorData(rawSensorData: RawSensorDataEntity)

    @Query("DELETE FROM raw_sensor_data")
    suspend fun deleteAllRawSensorData()
}
