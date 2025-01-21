package com.uoa.core.database.repository

import com.uoa.core.database.entities.QuestionnaireEntity
import kotlinx.coroutines.flow.Flow

interface QuestionnaireRepository {
    suspend fun saveResponseLocally(response: QuestionnaireEntity)
    suspend fun getAllQuestionnaireResponses(): Flow<List<QuestionnaireEntity>>
}