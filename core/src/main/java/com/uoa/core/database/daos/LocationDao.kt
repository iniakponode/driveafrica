package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert
    suspend fun insertLocation(location: LocationEntity)

    @Query("SELECT * FROM location WHERE id = :id")
    suspend fun getLocationById(id: Int): LocationEntity?

    @Query("SELECT * FROM location WHERE sync = 0")
    fun getUnsyncedLocations(): Flow<List<LocationEntity>>

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Query("DELETE FROM location")
    suspend fun deleteAllLocations()
}
