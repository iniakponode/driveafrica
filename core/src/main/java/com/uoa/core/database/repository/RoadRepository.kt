package com.uoa.core.database.repository

import com.uoa.core.database.daos.RoadDao
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.entities.RoadEntity
import com.uoa.core.model.Road
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface RoadRepository {
    fun getNearByRoad(latitude: Double, longitude: Double, radius: Double): List<Road>

    fun getRoadByCoordinates(latitude: Double, longitude: Double, radius: Double): Road?

    fun getRoadNameByCoordinates(latitude: Double, longitude: Double, radius: Double): String?

    fun getSpeedLimitByCoordinates(latitude: Double, longitude: Double, radius: Double): Int?
    fun getRoadsBySyncStatus(synced: Boolean): List<RoadEntity>
    fun updateRoad(road: RoadEntity)

    fun saveOrUpdateRoad(road: Road)

    fun getAllRoads(): List<Road>
//    suspend fun saveOrUpdateRoadRemotelyFirst(road: Road): Resource<Unit>
    suspend fun saveRoadLocally(road: Road): Resource<Unit>
}