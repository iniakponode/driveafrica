package com.uoa.core.database.repository

import com.uoa.core.model.LocationData
import com.uoa.core.model.Trip
import java.time.LocalDate
import java.util.UUID

interface TripDataRepository {
    suspend fun insertTrip(trip: Trip)

    suspend fun updateTrip(trip: Trip)

    suspend fun getAllTrips(): List<Trip>

    suspend fun getTripById(id: UUID): Trip?

    suspend fun getTripByIds(ids: List<UUID>): List<Trip>

    suspend fun updateUploadStatus(id: Int, sync: Boolean)

    suspend fun getTripsByDriverProfileId(driverProfileId: UUID): List<Trip>
    suspend fun getTripsBetweenDates(startDate: LocalDate, endDate: LocalDate): List<Trip>

    suspend fun getTripsBySyncStatus(synced: Boolean): List<Trip>

    suspend fun deleteTripById(id: UUID)
}