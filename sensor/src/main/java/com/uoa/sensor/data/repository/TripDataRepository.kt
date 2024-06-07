package com.uoa.sensor.data.repository

import com.uoa.core.database.daos.TripDao
import com.uoa.core.database.entities.TripEntity
import com.uoa.sensor.data.model.Trip
import com.uoa.sensor.data.toDomainModel
import com.uoa.sensor.data.toEntity
import javax.inject.Inject

class TripDataRepository @Inject constructor(private val tripDataDao: TripDao) {

    suspend fun insertTrip(trip: Trip): Long {
        return tripDataDao.insertTrip(trip.toEntity())
    }

    suspend fun updateTrip(trip: Trip) {
        tripDataDao.updateTrip(trip.toEntity())
    }

    suspend fun getAllTrips(): List<Trip> {
        return tripDataDao.getAllTrips().map { it.toDomainModel() }
    }
    suspend fun getTripById(id: Long): Trip? {
        return tripDataDao.getTripById(id)?.toDomainModel()
    }

    suspend fun updateUploadStatus(id: Int, sync: Boolean) {
        return tripDataDao.updateUploadStatus(id, sync)
    }

    suspend fun getTripsByDriverProfileId(driverProfileId: Long): List<Trip> {
        return tripDataDao.getTripsByDriverProfileId(driverProfileId).map { it.toDomainModel() }
    }

    suspend fun getTripsBySyncStatus(synced: Boolean): List<Trip> {
        return tripDataDao.getTripsBySyncStatus(synced).map { it.toDomainModel() }
    }

    suspend fun deleteTripById(id: Long) {
        tripDataDao.deleteTripById(id)
    }
}
