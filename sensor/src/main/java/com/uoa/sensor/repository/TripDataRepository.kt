package com.uoa.sensor.data.repository

import com.uoa.core.database.daos.TripDao
import com.uoa.core.model.Trip
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import java.util.UUID
import javax.inject.Inject

class TripDataRepository @Inject constructor(private val tripDataDao: TripDao) {

    suspend fun insertTrip(trip: Trip){
        return tripDataDao.insertTrip(trip.toEntity())
    }

    suspend fun updateTrip(trip: Trip) {
        tripDataDao.updateTrip(trip.toEntity())
    }

    suspend fun getAllTrips(): List<Trip> {
        return tripDataDao.getAllTrips().map { it.toDomainModel() }
    }
    suspend fun getTripById(id: UUID): Trip? {
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
