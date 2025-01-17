package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.uoa.core.database.entities.QuestionnaireEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

// DAO
@Dao
interface AlcoholQuestionnaireResponseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponse(response: QuestionnaireEntity)

    @Query("SELECT * FROM questionnaire_responses")
    fun getAllResponses(): Flow<List<QuestionnaireEntity>>

    @Query("SELECT * FROM questionnaire_responses WHERE driverProfileId = :userId")
    fun getQuestionnaireResponseByUserId(userId: UUID): Flow<QuestionnaireEntity>

    @Query("DELETE FROM questionnaire_responses WHERE driverProfileId = :userId")
    fun deleteQuestionnaireResponseByUserId(userId: UUID)
}