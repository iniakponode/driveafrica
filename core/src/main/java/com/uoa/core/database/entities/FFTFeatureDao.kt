package com.uoa.core.database.entities

// FFTFeatureDao.kt
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FFTFeatureDao {
    @Insert
    suspend fun insert(feature: FFTFeatureEntity)

    @Query("SELECT * FROM fft_features ORDER BY timestamp DESC")
    fun streamAll(): Flow<List<FFTFeatureEntity>>
}
