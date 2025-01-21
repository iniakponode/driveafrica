package com.uoa.alcoholquestionnaire.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
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
import java.util.UUID

@HiltViewModel
class QuestionnaireViewModel @javax.inject.Inject constructor(
    private val localRepository: QuestionnaireRepository,
    private val remoteRepository: QuestionnaireApiRepository,
    application: Application
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

    /**
     * Uploads the response to the remote server first. Only if the remote upload
     * succeeds do we save to local DB. We expose the status of the operation via [uploadState].
     */
    fun uploadResponseToServer(response: AlcoholQuestionnaireCreate) {
        viewModelScope.launch {
            // Start loading
            _uploadState.value =Resource.Loading

            try {
                // Log the entire response map
                Log.d("UploadResponse", "Uploading response: $response")
                // Attempt remote upload
                remoteRepository.uploadResponseToServer(response)

                // If successful, create an entity and save locally
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
                    date = DateConversionUtils.stringToDate(response.date)!!,
                    plansToDrive = response.plansToDrive
                )

                // Log entity details after creation
                Log.d("UploadResponse", "Entity created: $entity")

                localRepository.saveResponseLocally(entity)

                // Notify that the entire operation is successful
                _uploadState.value = Resource.Success(Unit)
            } catch (e: Exception) {

                // Log the error message
                Log.e("UploadResponse", "Error occurred: ${e.message}", e)
                // Something went wrong (either remote or local)
                _uploadState.value = Resource.Error(e.message ?: "An error occurred")
            }
        }
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