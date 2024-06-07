package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.DbdaResultEntity
import com.uoa.core.database.entities.SensorEntity

@Dao
interface DbdaResultDao {
    @Insert
    suspend fun insertDbdaResult(dbdaResultEntity: DbdaResultEntity): Long

    @Update
    suspend fun updateDbdaResult(dbdaResultEntity: DbdaResultEntity)

    @Query("SELECT * FROM driving_behavior_analysis")
    suspend fun getAllDbdaResults(): List<DbdaResultEntity>

    @Query("SELECT * FROM driving_behavior_analysis WHERE tripDataId = :tripDataId")
    suspend fun getDbdaResultByTripId(tripDataId: Long): List<DbdaResultEntity>

    @Query("SELECT * FROM driving_behavior_analysis WHERE causeUpdated = :c_updated")
    suspend fun getNoCauseUpdatedDbdaResult(c_updated: Boolean): DbdaResultEntity

    @Query("UPDATE driving_behavior_analysis SET causes = :causes WHERE id = :id")
    suspend fun updateCause(id: Int, causes: String)

    @Query("UPDATE driving_behavior_analysis SET synced = :sync WHERE id = :id")
    suspend fun updateUploadStatus(id: Int, sync: Boolean)

    @Query("SELECT * FROM driving_behavior_analysis WHERE userId = :userId")
    suspend fun getDbdaResultsByUserId(userId: String): List<DbdaResultEntity>

    @Query("SELECT * FROM driving_behavior_analysis WHERE synced = :synced")
    suspend fun getDbdaResultsBySyncStatus(synced: Boolean): List<DbdaResultEntity>

    @Query("DELETE FROM driving_behavior_analysis WHERE id = :id")
    suspend fun deleteDbdaResultById(id: Int)
}
