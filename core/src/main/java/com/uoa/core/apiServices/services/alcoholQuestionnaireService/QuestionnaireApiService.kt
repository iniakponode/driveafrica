package com.uoa.core.apiServices.services.alcoholQuestionnaireService

import com.uoa.core.apiServices.models.alcoholquestionnaireModels.AlcoholQuestionnaireCreate
import com.uoa.core.apiServices.models.alcoholquestionnaireModels.AlcoholQuestionnaireResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.UUID

// API Service
interface QuestionnaireApiService {
    @POST("api/questionnaire")
    suspend fun submitQuestionnaire(@Body request: AlcoholQuestionnaireCreate)

    @GET("api/questionnaireH/{userId}")
    suspend fun getQuestionnaireHistory(@Path("userId") userId: UUID): List<AlcoholQuestionnaireResponse>
}