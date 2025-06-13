package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.AIModelInputsEntity
import com.uoa.core.database.entities.LocationEntity
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.model.AIModelInputs
import com.uoa.core.utils.toDomainModel
import java.util.UUID

@Dao
interface AIModelInputDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiModelInput(aiModelInput: AIModelInputsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiModelInputBatch(aiModelInputList: List<AIModelInputsEntity>)


    @Query("DELETE FROM ai_model_inputs")
    suspend fun deleteAllAiModelInputs()

    @Query("SELECT * FROM ai_model_inputs")
    suspend fun getAllAiModelInputs(): List<AIModelInputsEntity>

    @Query("SELECT * FROM ai_model_inputs WHERE id = :id")
    suspend fun getAiModelInputById(id: UUID): AIModelInputsEntity?

    @Query("Select * FROM ai_model_inputs WHERE tripId = :tripId")
    suspend fun getAiModelInputsByTripId(tripId: UUID): List<AIModelInputs>

    @Query("Select * FROM ai_model_inputs WHERE sync= :status")
    suspend fun getAiModelInputsBySyncStatus(status: Boolean): List<AIModelInputsEntity>

    @Query("SELECT * FROM ai_model_inputs WHERE sync= :synced AND processed= :processed")
    suspend fun getAiModelInputBySyncAndProcessedStatus(synced: Boolean, processed: Boolean): List<AIModelInputsEntity>


    @Update
    suspend fun updateAiModelInput(aiModelInput: AIModelInputsEntity)

    @Query("DELETE FROM ai_model_inputs")
    suspend fun deleteAiModelInput()

    suspend fun deleteAiModelInputById(id: UUID) {
        val aiModelInputs = getAiModelInputById(id)
        if (aiModelInputs != null) {
            deleteAiModelInput()
        }
    }

    @Query("DELETE FROM ai_model_inputs WHERE id IN (:ids)")
    suspend fun deleteAIModelInputsByIds(ids: List<UUID>)
}