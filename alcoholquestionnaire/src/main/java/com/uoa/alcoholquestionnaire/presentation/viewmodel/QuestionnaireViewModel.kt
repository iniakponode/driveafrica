package com.uoa.alcoholquestionnaire.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.net.http.HttpException
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresExtension
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.apiServices.models.alcoholquestionnaireModels.AlcoholQuestionnaireCreate
import com.uoa.core.apiServices.models.alcoholquestionnaireModels.AlcoholQuestionnaireResponse
import com.uoa.core.apiServices.services.alcoholQuestionnaireService.QuestionnaireApiRepository
import com.uoa.core.database.entities.QuestionnaireEntity
import com.uoa.core.database.repository.QuestionnaireRepository
import com.uoa.core.model.Questionnaire
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.DateConversionUtils
import kotlinx.coroutines.launch
import com.uoa.core.utils.Resource
import com.uoa.core.utils.toQuestionnaire
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.Job
import com.uoa.core.network.NetworkMonitor
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class QuestionnaireViewModel @javax.inject.Inject constructor(
    private val localRepository: QuestionnaireRepository,
    private val remoteRepository: QuestionnaireApiRepository,
    application: Application,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val appContext = application.applicationContext
    val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedProfileId = prefs.getString(DRIVER_PROFILE_ID, null)

    /**
     * LiveData (or StateFlow) for tracking the status of the upload+local-save operation.
     * You can observe this in the UI to know if the operation is loading, successful, or errored.
     */
    private val _uploadState = MutableLiveData<Resource<Unit>>()
    val uploadState: LiveData<Resource<Unit>> = _uploadState

    /**
     * LiveData (or StateFlow) for tracking the status of fetching questionnaire history.
     */
    private val _historyState = MutableLiveData<Resource<List<Questionnaire>>>()
    val historyState: LiveData<Resource<List<Questionnaire>>> = _historyState

//    /**
//     * Uploads the response to the remote server first. Only if the remote upload
//     * succeeds do we save to local DB. We expose the status of the operation via [uploadState].
//     */
//    fun uploadResponseToServer(response: AlcoholQuestionnaireCreate) {
//        viewModelScope.launch {
//            // Start loading
//            _uploadState.value =Resource.Loading
//
//            try {
//                // Log the entire response map
//                Log.d("UploadResponse", "Uploading response: $response")
//                // Attempt remote upload
//                remoteRepository.uploadResponseToServer(response)
//
//                // If successful, create an entity and save locally
//                val entity = QuestionnaireEntity(
//                    id = response.id,
//                    driverProfileId = response.driverProfileId,
//                    drankAlcohol = response.drankAlcohol,
//                    selectedAlcoholTypes = response.selectedAlcoholTypes,
//                    beerQuantity = response.beerQuantity,
//                    wineQuantity = response.wineQuantity,
//                    spiritsQuantity = response.spiritsQuantity,
//                    firstDrinkTime = response.firstDrinkTime,
//                    lastDrinkTime = response.lastDrinkTime,
//                    emptyStomach = response.emptyStomach,
//                    caffeinatedDrink = response.caffeinatedDrink,
//                    impairmentLevel = response.impairmentLevel,
//                    date = DateConversionUtils.stringToDate(response.date)!!,
//                    plansToDrive = response.plansToDrive
//                )
//
//                // Log entity details after creation
//                Log.d("UploadResponse", "Entity created: $entity")
//
//                localRepository.saveResponseLocally(entity)
//
//                // Notify that the entire operation is successful
//                _uploadState.value = Resource.Success(Unit)
//            } catch (e: Exception) {
//
//                // Log the error message
//                Log.e("UploadResponse", "Error occurred: ${e.message}", e)
//                // Something went wrong (either remote or local)
//                _uploadState.value = Resource.Error(e.message ?: "An error occurred")
//            }
//        }
//    }

    /**
     * 1) Save response locally.
     * 2) Attempt remote upload ONCE.
     *    - If success, mark as synced in local DB.
     *    - If data integrity error, skip/mark invalid so it won't be retried.
     *    - If any other error, do NOT mark as synced => Worker will handle.
     */
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    fun saveAndAttemptUpload(response: AlcoholQuestionnaireCreate) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC+1")

        // Create a Job to track the task and avoid cancellation on navigation
        val uploadJob = Job()

        // Launch the coroutine with the created job to allow manual cancellation control
        viewModelScope.launch(uploadJob) {
            Log.d("QuestionnaireViewModel", "Attempting to save response: $response")
            _uploadState.value = Resource.Loading

            try {
                // Convert to entity
                val entity = QuestionnaireEntity(
                    id = response.id,
                    driverProfileId = response.driverProfileId,
                    drankAlcohol = response.drankAlcohol,
                    selectedAlcoholTypes = response.selectedAlcoholTypes,
                    beerQuantity = response.beerQuantity,
                    wineQuantity = response.wineQuantity,
                    spiritsQuantity = response.spiritsQuantity,
                    firstDrinkTime = response.firstDrinkTime,
                    lastDrinkTime = response.lastDrinkTime,
                    emptyStomach = response.emptyStomach,
                    caffeinatedDrink = response.caffeinatedDrink,
                    impairmentLevel = response.impairmentLevel,
                    date = DateConversionUtils.stringToDate(response.date) ?: Date(),
                    plansToDrive = response.plansToDrive,
                    sync = false,    // Initially unsynced
                )
                Log.d("QuestionnaireViewModel", "Entity created: $entity")

                // Save locally
                localRepository.saveResponseLocally(entity)
                _uploadState.value = Resource.Success(Unit)

                // Attempt upload if network is available
                if (networkMonitor.isOnline.first()) {
                    try {
                        val originalDate: Date? = DateConversionUtils.stringToDate(response.date)
                        val formattedDate = originalDate?.let { sdf.format(it) } ?: response.date
                        val responseForUpload = response.copy(date = formattedDate, sync = true)
                        Log.d("QuestionnaireViewModel", "Uploading response: $responseForUpload")

                        // Upload and handle response
                        val uploadResult = remoteRepository.uploadResponseToServer(responseForUpload)

                        // If successful, mark as synced
                        if (uploadResult is Resource.Success) {
                            localRepository.markAsSynced(entity.id, true)
                            _uploadState.value = Resource.Success(Unit)
                        } else if (uploadResult is Resource.Error) {
                            _uploadState.value = Resource.Error(uploadResult.message ?: "Unknown error.")
                        }

                    } catch (e: CancellationException) {
                        // Explicitly handle job cancellation due to navigation away
                        Log.e("QuestionnaireViewModel", "Upload cancelled due to navigation: ${e.message}")
                        _uploadState.value = Resource.Error("Upload cancelled due to navigation.")
                    } catch (e: IOException) {
                        Log.e("QuestionnaireViewModel", "Network error while uploading: ${e.message}")
                        _uploadState.value = Resource.Error("Network error: Please check your internet connection.")
                    } catch (e: HttpException) {
                        Log.e("QuestionnaireViewModel", "Server error: ${e.message}")
                        _uploadState.value = Resource.Error("Server error: ${e.message}")
                    } catch (e: Exception) {
                        Log.e("QuestionnaireViewModel", "Unexpected error: ${e.message}", e)
                        _uploadState.value = Resource.Error("An unexpected error occurred: ${e.message}")
                    }
                } else {
                    // No network => user sees immediate error, worker will pick it up
                    Log.e("QuestionnaireViewModel", "No internet connection.")
                    _uploadState.value = Resource.Error("No internet. Will retry in background.")
                }
            } catch (ex: Exception) {
                Log.e("QuestionnaireViewModel", "Local save error: ${ex.message}", ex)
                _uploadState.value = Resource.Error(ex.message ?: "Local save error.")
            }
        }
    }



    private fun isDataIntegrityError(e: Exception): Boolean {
        // Implement logic to detect a data integrity problem from the exception
        // For example, check if it's a 400 Bad Request from server
        return e.message?.contains("Integrity") == true ||
                e.message?.contains("Invalid data") == true
    }

    /**
     * Fetches questionnaire history from the remote repository.
     * Exposes the loading/success/error state via [historyState].
     */
    fun fetchQuestionnaireHistory(userId: UUID){
        viewModelScope.launch {
            // Start loading
            _historyState.value = Resource.Loading

            try {
                val responseList: List<AlcoholQuestionnaireResponse> =
                    remoteRepository.fetchQuestionnaireHistory(userId)

                val convertedList = responseList.map { it.toQuestionnaire() }
                // Successfully fetched
                _historyState.value = Resource.Success(convertedList)
            } catch (e: Exception) {
                // Error fetching
                _historyState.value = Resource.Error(e.message ?: "Failed to fetch history")
            }
        }
    }
}