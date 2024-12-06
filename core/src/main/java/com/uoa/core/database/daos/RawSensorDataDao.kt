package com.uoa.core.database.daos
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.entities.SensorEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.util.Date
import java.util.UUID

@Dao
interface RawSensorDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawSensorData(rawSensorData: RawSensorDataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawSensorDataBatch(rawSensorDataList: List<RawSensorDataEntity>)

    @Query("""
        SELECT * FROM raw_sensor_data 
        WHERE tripId = :tripId 
        AND locationId IS NOT NULL
        ORDER BY timestamp ASC
    """)
    fun getRawSensorDataByTripId(tripId: UUID): Flow<List<RawSensorDataEntity>>

    @Query("""
        SELECT * FROM raw_sensor_data
        WHERE tripId = :tripId
        AND locationId IS NOT NULL
        AND (:lastId IS NULL OR id > :lastId)
        ORDER BY id ASC
        LIMIT :limit
    """)
    suspend fun getRawSensorDataChunkAfterId(
        tripId: UUID,
        limit: Int,
        lastId: UUID?
    ): List<RawSensorDataEntity>

    @Query("SELECT * FROM raw_sensor_data WHERE locationId = :locationId")
    fun getRawSensorDataByLocationId(locationId: UUID): Flow<List<RawSensorDataEntity>>

    @Query("SELECT * FROM raw_sensor_data WHERE date BETWEEN :startDate AND :endDate")
    fun getRawSensorDataBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<RawSensorDataEntity>>

    @Query("SELECT * FROM raw_sensor_data WHERE sync = :synced")
    suspend fun getSensorDataBySyncStatus(synced: Boolean): List<RawSensorDataEntity>

    @Query("SELECT * FROM raw_sensor_data WHERE id = :id")
    suspend fun getRawSensorDataById(id: UUID): RawSensorDataEntity?

    @Query("SELECT * FROM raw_sensor_data WHERE sync = 0")
    fun getUnsyncedRawSensorData(): Flow<List<RawSensorDataEntity>>

    @Update
    suspend fun updateRawSensorData(rawSensorData: RawSensorDataEntity)

    @Query("DELETE FROM raw_sensor_data")
    suspend fun deleteAllRawSensorData()

    @Query("SELECT * FROM raw_sensor_data WHERE date BETWEEN :startDate AND :endDate")
    fun getSensorDataBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<RawSensorDataEntity>>

    @Query("SELECT * FROM raw_sensor_data WHERE tripId = :tripId")
    fun getSensorDataByTripId(tripId: UUID): Flow<List<RawSensorDataEntity>>

    @Query("SELECT * FROM raw_sensor_data WHERE date BETWEEN :startDate AND :endDate LIMIT :limit OFFSET :offset")
    suspend fun getRawSensorDataPaginated(startDate: Date, endDate: Date, limit: Int, offset: Int): List<RawSensorDataEntity>
}
