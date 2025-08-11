package com.uoa.sensor.repository

import android.util.Log
import com.uoa.core.apiServices.models.roadModels.RoadCreate
import com.uoa.core.apiServices.services.roadApiService.RoadApiRepository
import com.uoa.core.database.daos.RoadDao
import com.uoa.core.database.entities.RoadEntity
import com.uoa.core.database.repository.RoadRepository
import com.uoa.core.model.Road
import com.uoa.core.utils.Resource
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class RoadRepositoryImpl @Inject constructor(private val roadDao: RoadDao, private val roadApiRepository: RoadApiRepository): RoadRepository{
    override fun getNearByRoad(
        latitude: Double,
        longitude: Double,
        radius: Double
    ): List<Road> {
        return roadDao.getNearbyRoads(latitude, longitude, radius).map { it.toDomainModel() }
    }

    override fun getRoadByCoordinates(
        latitude: Double,
        longitude: Double,
        radius: Double
    ): Road? {
        // Retrieve roads within the given radius and pick the nearest or first match
        val roads = roadDao.getNearbyRoads(latitude, longitude, radius)
        return roads.firstOrNull()?.toDomainModel()
    }

    override fun getRoadNameByCoordinates(
        latitude: Double,
        longitude: Double,
        radius: Double
    ): String? {
        return getRoadByCoordinates(latitude, longitude, radius)?.name
    }

    override fun getSpeedLimitByCoordinates(
        latitude: Double,
        longitude: Double,
        radius: Double
    ): Int? {
        return getRoadByCoordinates(latitude, longitude, radius)?.speedLimit
    }

    override fun saveOrUpdateRoad(road: Road) {
        // Insert or update road information for persistent caching
        roadDao.insertOrUpdate(road.toEntity())
    }

    override fun getAllRoads(): List<Road> {
        return roadDao.getAllRoads().map { it.toDomainModel() }
    }

    override fun getRoadById(id:UUID): Road{
        return roadDao.getRoadById(id)
    }

//    override suspend fun saveOrUpdateRoadRemotelyFirst(road: Road): Resource<Unit> = withContext(Dispatchers.IO) {
//        try {
//            // Convert Road to RoadCreate payload
//            val roadCreate = RoadCreate(
//                id = road.id,
//                driverProfileId = road.driverProfileId,
//                name = road.name,
//                roadType = road.roadType,
//                speedLimit = road.speedLimit,
//                latitude = road.latitude,
//                longitude = road.longitude,
//            )
//
//            // Attempt to create or update the road remotely
//            val remoteResult = roadApiRepository.createRoad(roadCreate)
//            if (remoteResult is Resource.Success) {
//                // If remote save succeeds, persist locally
//                roadDao.insertOrUpdate(road.toEntity())
//                Resource.Success(Unit)
//            } else if (remoteResult is Resource.Error) {
//                Resource.Error("Remote save failed: ${remoteResult.message}")
//            } else {
//                Resource.Error("Remote save failed with unknown error.")
//            }
//        } catch (e: Exception) {
//            Resource.Error("Unexpected error: ${e.localizedMessage}")
//        }
//    }

    override suspend fun saveRoadLocally(road: Road): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            // Attempt to persist the road entity locally
            roadDao.insertOrUpdate(road.toEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            // Log the error and return an error resource
            Log.e("RoadRepository", "Error saving road locally: ${e.localizedMessage}", e)
            Resource.Error("Local save failed: ${e.localizedMessage}")
        }
    }

    override fun getRoadsBySyncStatus(synced: Boolean): List<RoadEntity> {
        return roadDao.getRoadsBySyncStatus(synced)
    }

    override fun updateRoad(road: RoadEntity) {
        roadDao.updateRoad(road)
    }

    override suspend fun markSyncByIds(ids: List<UUID>, sync: Boolean) = roadDao.markSyncByIds(ids, sync)




}