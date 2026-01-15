package com.uoa.core.apiServices.services.alcoholQuestionnaireService

import com.uoa.core.apiServices.models.alcoholquestionnaireModels.AlcoholQuestionnaireCreate
import com.uoa.core.apiServices.models.alcoholquestionnaireModels.AlcoholQuestionnaireResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.UUID

// API Service
interface QuestionnaireApiService {
    @POST("/api/questionnaires/")
    suspend fun submitQuestionnaire(@Body request: AlcoholQuestionnaireCreate): Response<AlcoholQuestionnaireResponse>

    @POST("/api/questionnaires/batch_create")
    suspend fun submitQuestionnaires(@Body request: List<AlcoholQuestionnaireCreate>): Map<String, Int>

    @GET("/api/questionnaires/{userId}")
    suspend fun getQuestionnaireHistory(@Path("userId") userId: UUID): List<AlcoholQuestionnaireResponse>

    @POST("/api/questionnaire/")
    suspend fun submitQuestionnaireLegacy(@Body request: AlcoholQuestionnaireCreate): Response<AlcoholQuestionnaireResponse>

    @POST("/api/questionnaire/batch_create")
    suspend fun submitQuestionnairesLegacy(@Body request: List<AlcoholQuestionnaireCreate>): Map<String, Int>

    @GET("/api/questionnaire/{userId}")
    suspend fun getQuestionnaireHistoryLegacy(@Path("userId") userId: UUID): List<AlcoholQuestionnaireResponse>
}
