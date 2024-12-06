package com.uoa.core.database.repository

import com.uoa.core.database.daos.RoadDao
import com.uoa.core.model.Road

interface RoadRepository {
    fun getNearByRoad(latitude: Double, longitude: Double, radius: Double): List<Road>
}