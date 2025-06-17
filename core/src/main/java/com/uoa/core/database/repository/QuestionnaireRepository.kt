package com.uoa.core.database.repository

import com.uoa.core.database.entities.QuestionnaireEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface QuestionnaireRepository {
    suspend fun saveResponseLocally(response: QuestionnaireEntity)
    suspend fun getAllQuestionnaireResponses(): Flow<List<QuestionnaireEntity>>
//    suspend fun saveQuestionnaire(entity: QuestionnaireEntity)
    suspend fun markAsSynced(questionnaireId: UUID, status: Boolean)
//    suspend fun markAsInvalid(questionnaireId: UUID)
    suspend fun getAllUnsyncedQuestionnaires(): List<QuestionnaireEntity>
}