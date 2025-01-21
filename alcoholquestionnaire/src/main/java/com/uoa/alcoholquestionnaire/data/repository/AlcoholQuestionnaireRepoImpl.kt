package com.uoa.alcoholquestionnaire.data.repository

import com.uoa.core.database.daos.AlcoholQuestionnaireResponseDao
import com.uoa.core.database.entities.QuestionnaireEntity
import com.uoa.core.database.repository.QuestionnaireRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AlcoholQuestionnaireRepoImpl (private val dao: AlcoholQuestionnaireResponseDao): QuestionnaireRepository {
    override suspend fun saveResponseLocally(response: QuestionnaireEntity) {
        withContext(Dispatchers.IO) {
            dao.insertResponse(response)
        }
    }

    override suspend fun getAllQuestionnaireResponses(): Flow<List<QuestionnaireEntity>> {
        return withContext(Dispatchers.IO) {
            dao.getAllResponses()
        }

    }

}