package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.RoadEntity
import com.uoa.core.model.Road

@Dao
interface RoadDao {
    @Query("SELECT * FROM roads WHERE ABS(latitude - :latitude) < :radius AND ABS(longitude - :longitude) < :radius")
    fun getNearbyRoads(latitude: Double, longitude: Double, radius: Double): List<RoadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(road: RoadEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateRoad(road: RoadEntity)

    @Query("SELECT * FROM roads WHERE synced= :syncStat")
    fun getRoadsBySyncStatus(syncStat: Boolean): List<RoadEntity>

    @Query("SELECT * FROM roads")
    fun getAllRoads(): List<RoadEntity>
}


