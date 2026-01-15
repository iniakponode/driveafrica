package com.uoa.core.apiServices.services.alcoholQuestionnaireService

import com.uoa.core.apiServices.models.alcoholquestionnaireModels.AlcoholQuestionnaireCreate
import com.uoa.core.apiServices.models.alcoholquestionnaireModels.AlcoholQuestionnaireResponse
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class QuestionnaireApiRepository@Inject constructor(
    private val questionnaireApiService: QuestionnaireApiService
) {
    suspend fun uploadResponseToServer(questionnaire: AlcoholQuestionnaireCreate): Resource<AlcoholQuestionnaireResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Attempt the upload request
                val response = questionnaireApiService.submitQuestionnaire(questionnaire)

                // Check if the response is successful
                return@withContext if (response.isSuccessful) {
                    // Return the response body if the request is successful
                    Resource.Success(response.body()!!)
                } else if (response.code() == 404) {
                    val legacyResponse = questionnaireApiService.submitQuestionnaireLegacy(questionnaire)
                    if (legacyResponse.isSuccessful) {
                        Resource.Success(legacyResponse.body()!!)
                    } else {
                        Resource.Error("Failed to upload Questionnaire: ${legacyResponse.message()}")
                    }
                } else {
                    // Handle the case where the response is not successful
                    Resource.Error("Failed to upload Questionnaire: ${response.message()}")
                }
            } catch (e: IOException) {
                // Handle network-related errors
                Resource.Error("Network error: Please check your internet connection.")
            } catch (e: HttpException) {
                // Handle server-side errors (e.g., 500 or 404)
                Resource.Error("Server error (${e.code()}): ${e.message()}")
            } catch (e: Exception) {
                // Handle any other unexpected errors
                Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
            }
        }
    }



    suspend fun uploadResponsesToServer(questionnaires: List<AlcoholQuestionnaireCreate>): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                questionnaireApiService.submitQuestionnaires(questionnaires)
                Resource.Success(Unit)
            } catch (e: IOException) {
                Resource.Error("Network error: Please check your internet connection.")
            } catch (e: HttpException) {
                if (e.code() == 404) {
                    try {
                        questionnaireApiService.submitQuestionnairesLegacy(questionnaires)
                        Resource.Success(Unit)
                    } catch (legacyException: HttpException) {
                        Resource.Error("Server error (${legacyException.code()}): ${legacyException.message()}")
                    } catch (legacyException: Exception) {
                        Resource.Error("An unexpected error occurred: ${legacyException.localizedMessage}")
                    }
                } else {
                    Resource.Error("Server error (${e.code()}): ${e.message()}")
                }
            } catch (e: Exception) {
                Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
            }
        }
    }

        suspend fun fetchQuestionnaireHistory(userId: UUID): List<AlcoholQuestionnaireResponse> {
        return withContext(Dispatchers.IO) {
            try {
                questionnaireApiService.getQuestionnaireHistory(userId)
            } catch (e: HttpException) {
                if (e.code() == 404) {
                    questionnaireApiService.getQuestionnaireHistoryLegacy(userId)
                } else {
                    throw e
                }
            }
        }
    }
}
