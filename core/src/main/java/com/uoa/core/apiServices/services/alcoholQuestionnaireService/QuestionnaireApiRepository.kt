package com.uoa.core.apiServices.services.alcoholQuestionnaireService

import com.uoa.core.apiServices.models.alcoholquestionnaireModels.AlcoholQuestionnaireCreate
import com.uoa.core.apiServices.models.alcoholquestionnaireModels.AlcoholQuestionnaireResponse
import com.uoa.core.apiServices.services.aiModellInputApiService.AIModelInputApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class QuestionnaireApiRepository@Inject constructor(
    private val questionnaireApiService: QuestionnaireApiService
) {
    suspend fun uploadResponseToServer(response: AlcoholQuestionnaireCreate) {
        withContext(Dispatchers.IO) {
            questionnaireApiService.submitQuestionnaire(response)
        }
    }

    suspend fun fetchQuestionnaireHistory(userId: UUID): List<AlcoholQuestionnaireResponse> {
        return withContext(Dispatchers.IO) {
            questionnaireApiService.getQuestionnaireHistory(userId)
        }
    }
}