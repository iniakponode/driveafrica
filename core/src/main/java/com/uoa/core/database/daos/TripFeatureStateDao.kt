package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.uoa.core.database.entities.TripFeatureStateEntity
import java.util.UUID

@Dao
interface TripFeatureStateDao {
    @Query("SELECT * FROM trip_feature_state WHERE tripId = :tripId LIMIT 1")
    suspend fun getByTripId(tripId: UUID): TripFeatureStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: TripFeatureStateEntity)

    @Query("DELETE FROM trip_feature_state WHERE tripId = :tripId")
    suspend fun deleteByTripId(tripId: UUID)
}
