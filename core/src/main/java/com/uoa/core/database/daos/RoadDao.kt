package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.uoa.core.model.Road

@Dao
interface RoadDao {
    @Query("SELECT * FROM roads WHERE ABS(latitude - :latitude) < :radius AND ABS(longitude - :longitude) < :radius")
    fun getNearbyRoads(latitude: Double, longitude: Double, radius: Double): List<Road>
}

