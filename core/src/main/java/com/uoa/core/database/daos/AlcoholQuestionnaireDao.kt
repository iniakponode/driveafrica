package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.uoa.core.database.entities.AIModelInputsEntity
import com.uoa.core.database.entities.QuestionnaireEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.UUID

// DAO
@Dao
interface AlcoholQuestionnaireResponseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponse(response: QuestionnaireEntity)

    @Query("SELECT * FROM questionnaire_responses")
    fun getAllResponses(): Flow<List<QuestionnaireEntity>>

    @Query("UPDATE questionnaire_responses SET sync = :status WHERE id = :questionnaireId")
    suspend fun updateSyncStatus(questionnaireId: UUID, status: Boolean)

    @Query("UPDATE questionnaire_responses SET sync = :newStatus WHERE sync = :currentStatus")
    suspend fun updateAllSyncStatus(currentStatus: Boolean, newStatus: Boolean)


    @Query("SELECT * FROM questionnaire_responses WHERE driverProfileId = :userId")
    fun getQuestionnaireResponseByUserId(userId: UUID): Flow<QuestionnaireEntity>

    @Query("DELETE FROM questionnaire_responses WHERE driverProfileId = :userId")
    fun deleteQuestionnaireResponseByUserId(userId: UUID)

    @Query("Select * FROM questionnaire_responses WHERE sync= :status")
    suspend fun getQuestionnaireResponseSyncStatus(status: Boolean): List<QuestionnaireEntity>

    @Query("""
        SELECT * FROM questionnaire_responses
        WHERE driverProfileId = :userId
        AND date = :date
        LIMIT 1
    """)
    suspend fun getQuestionnaireResponseForDate(userId: UUID, date: Date): QuestionnaireEntity?
}
