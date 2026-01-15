package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.RoadEntity
import com.uoa.core.model.Road
import java.util.UUID

@Dao
interface RoadDao {
    @Query("SELECT * FROM roads WHERE ABS(latitude - :latitude) < :radius AND ABS(longitude - :longitude) < :radius ORDER BY ((latitude - :latitude) * (latitude - :latitude) + (longitude - :longitude) * (longitude - :longitude)) ASC")
    fun getNearbyRoads(latitude: Double, longitude: Double, radius: Double): List<RoadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(road: RoadEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateRoad(road: RoadEntity)

    @Query("SELECT * FROM roads WHERE sync= :syncStat")
    fun getRoadsBySyncStatus(syncStat: Boolean): List<RoadEntity>

    @Query("SELECT * FROM roads WHERE id= :id")
    fun getRoadById(id: UUID): Road

    @Query("SELECT * FROM roads")
    fun getAllRoads(): List<RoadEntity>

    @Query("UPDATE roads SET sync = :sync WHERE id IN (:ids)")
    suspend fun markSyncByIds(ids: List<UUID>, sync: Boolean)
}


