package com.uoa.core.apiServices.services.alcoholQuestionnaireService

import android.util.Log
import com.uoa.core.apiServices.models.alcoholquestionnaireModels.AlcoholQuestionnaireCreate
import com.uoa.core.apiServices.models.alcoholquestionnaireModels.AlcoholQuestionnaireResponse
import com.uoa.core.apiServices.services.aiModellInputApiService.AIModelInputApiService
import com.uoa.core.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketException
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class QuestionnaireApiRepository@Inject constructor(
    private val questionnaireApiService: QuestionnaireApiService
) {
    suspend fun uploadResponseToServer(response: AlcoholQuestionnaireCreate): Resource<AlcoholQuestionnaireResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Attempt the upload request
                val response = questionnaireApiService.submitQuestionnaire(response)

                // Check if the response is successful
                return@withContext if (response.isSuccessful) {
                    // Return the response body if the request is successful
                    Resource.Success(response.body()!!)
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
            return withContext(Dispatchers.IO) {  // Ensuring the upload is done in the IO dispatcher
                try {
                    var successCount = 0
                    var failureCount = 0

                    // Loop through each questionnaire and upload individually
                    for (questionnaire in questionnaires) {
                        try {
                            // Assuming that submitQuestionnaire returns a Response<AlcoholQuestionnaireResponse>
                            val response = questionnaireApiService.submitQuestionnaire(questionnaire)

                            // If the upload is successful, increment the success counter
                            if (response.isSuccessful) {
                                successCount++
                            } else {
                                failureCount++
                                Log.e("UploadQuestionnaires", "Failed to upload Questionnaire ${questionnaire.id}: ${response.message()}")
                            }
                        } catch (e: IOException) {
                            failureCount++
                            Log.e("UploadQuestionnaires", "Network error while uploading Questionnaire ${questionnaire.id}: ${e.localizedMessage}")
                        } catch (e: HttpException) {
                            failureCount++
                            Log.e("UploadQuestionnaires", "Server error (${e.code()}) while uploading Questionnaire ${questionnaire.id}: ${e.message()}")
                        } catch (e: Exception) {
                            failureCount++
                            Log.e("UploadQuestionnaires", "Unexpected error while uploading Questionnaire ${questionnaire.id}: ${e.localizedMessage}")
                        }
                    }

                    // If we have any successful uploads, return success
                    return@withContext if (failureCount == 0) {
                        Resource.Success(Unit)
                    } else {
                        // If we had some failures, we can return a Resource.Error
                        Resource.Error("Failed to upload $failureCount questionnaire(s)")
                    }

                } catch (e: Exception) {
                    // Catch any unexpected errors in the overall function
                    return@withContext Resource.Error("An unexpected error occurred: ${e.localizedMessage}")
                }
            }
        }

        suspend fun fetchQuestionnaireHistory(userId: UUID): List<AlcoholQuestionnaireResponse> {
        return withContext(Dispatchers.IO) {
            questionnaireApiService.getQuestionnaireHistory(userId)
        }
    }
}