package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.LocationEntity
import com.uoa.core.database.entities.TripEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Query("SELECT * FROM Location WHERE id IN (:ids)")
    suspend fun getLocationsByIds(ids: List<UUID>): List<LocationEntity>

    @Query("SELECT * FROM location")
    suspend fun getAllLocations(): List<LocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationBatch(locationList: List<LocationEntity>)

    @Query("SELECT * FROM location WHERE id = :id")
    suspend fun getLocationById(id: UUID): LocationEntity?

    @Query("SELECT * FROM location WHERE sync = :synced")
    fun getLocationBySyncStatus(synced: Boolean): Flow<List<LocationEntity>>

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Query("DELETE FROM location")
    suspend fun deleteAllLocations()

//    @Query("SELECT * FROM location WHERE tripId = :tripId")
//    fun getLocationsByTripId(tripId: UUID): Flow<List<LocationEntity>>
}