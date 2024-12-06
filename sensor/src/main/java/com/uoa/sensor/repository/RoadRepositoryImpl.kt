package com.uoa.sensor.repository

import com.uoa.core.database.daos.RoadDao
import com.uoa.core.database.repository.RoadRepository
import com.uoa.core.model.Road
import javax.inject.Inject

class RoadRepositoryImpl @Inject constructor(private val roadDao: RoadDao): RoadRepository{
    override fun getNearByRoad(
        latitude: Double,
        longitude: Double,
        radius: Double
    ): List<Road> {
        return roadDao.getNearbyRoads(latitude, longitude, radius)
    }
}