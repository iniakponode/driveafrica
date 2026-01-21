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

    @Query("SELECT * FROM trip_feature_state WHERE sync = 0")
    suspend fun getUnsyncedTripFeatureStates(): List<TripFeatureStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: TripFeatureStateEntity)

    @Query("UPDATE trip_feature_state SET sync = :synced WHERE tripId IN (:tripIds)")
    suspend fun markTripFeatureStatesSynced(tripIds: List<UUID>, synced: Boolean)

    @Query("DELETE FROM trip_feature_state WHERE tripId = :tripId")
    suspend fun deleteByTripId(tripId: UUID)
}
