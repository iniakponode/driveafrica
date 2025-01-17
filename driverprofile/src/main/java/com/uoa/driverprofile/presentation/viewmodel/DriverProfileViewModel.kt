package com.uoa.driverprofile.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.apiServices.models.driverProfile.DriverProfileCreate
import com.uoa.core.apiServices.services.driverProfileApiService.DriverProfileApiRepository
import com.uoa.core.database.entities.DriverProfileEntity
import com.uoa.core.utils.Resource
//import com.uoa.core.database.entities.EmbeddingEntity
//import com.uoa.core.nlg.repository.EmbeddingUtilsRepository
//import com.uoa.core.nlg.RAGEngine
//import com.uoa.core.utils.EmbeddingUtils.serializeEmbedding
import com.uoa.driverprofile.R
import com.uoa.driverprofile.domain.usecase.DeleteDriverProfileByEmailUseCase
import com.uoa.driverprofile.domain.usecase.GetDriverProfileByEmailUseCase
import com.uoa.driverprofile.domain.usecase.InsertDriverProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DriverProfileViewModel @Inject constructor(
    private val insertDriverProfileUseCase: InsertDriverProfileUseCase,
    private val getDriverProfileByEmailUseCase: GetDriverProfileByEmailUseCase,
    private val deleteDriverProfileByEmailUseCase: DeleteDriverProfileByEmailUseCase,
    private val driverProfileApiRepository: DriverProfileApiRepository,
//    private val embeddingUtilsRepository: EmbeddingUtilsRepository,

//    private val ragEngine: RAGEngine,
    private val application: Application,
) : ViewModel() {

    private val _email = MutableLiveData<String>()
    val email: MutableLiveData<String> get() = _email

    private val _profile_id = MutableLiveData<String>()
    val profile_id: MutableLiveData<String> get() = _profile_id

    private val _driverProfileUploadSuccess = MutableStateFlow(false)
    val driverProfileUploadSuccess: StateFlow<Boolean> get() = _driverProfileUploadSuccess.asStateFlow()

//    init {
//        Log.d("DriverProfileViewModel", "Initializing DriverProfileViewModel")
//        // Run the embedding generation task during initialization
//        viewModelScope.launch {
//            val all_embeddings = embeddingUtilsRepository.getAllEmbeddings()
//            if (all_embeddings.isEmpty()) {
//                generateAndStorePdfEmbeddings()
//            }
//            else {
//                Log.d("DriverProfileViewModel", "Embeddings already exist in the database")
//            }
//        }
//    }

    // Function to generate and store embeddings in the database
//    private suspend fun generateAndStorePdfEmbeddings() {
//        withContext(Dispatchers.IO) {
//            Log.d("DriverProfileViewModel", "Starting generateAndStorePdfEmbeddings")
//            val context = application.applicationContext
//
//            // Initialize RAGEngine session and tokenizer
//            ragEngine.initialize(context)
//
//            try {
//                // Extract text from PDFs
//                val lawText = ragEngine.extractTextFromPdf(context, R.raw.nat_dr_reg_law)
//                Log.d("DriverProfileViewModel", "Extracted lawText length: ${lawText.length}")
//                val codeText = ragEngine.extractTextFromPdf(context, R.raw.ng_high_way_code)
//                Log.d("DriverProfileViewModel", "Extracted codeText length: ${codeText.length}")
//
//                // Process chunks and store embeddings
//                processAndStoreChunks(lawText, "nat_dr_reg_law")
//                processAndStoreChunks(codeText, "ng_high_way_code")
//                Log.d("DriverProfileViewModel", "Finished generateAndStorePdfEmbeddings")
//            } catch (e: Exception) {
//                Log.e("DriverProfileViewModel", "Error during embedding generation", e)
//            } finally {
//                // Close RAGEngine session
//                ragEngine.close()
//            }
//        }
//    }
//
//    private suspend fun processAndStoreChunks(text: String, sourceType: String) {
//        Log.d("DriverProfileViewModel", "Starting processAndStoreChunks for sourceType: $sourceType")
//        val chunks = ragEngine.chunkText(text)
//        Log.d("DriverProfileViewModel", "Number of chunks to process: ${chunks.size}")
//        chunks.forEachIndexed { index, chunk ->
//            Log.d("DriverProfileViewModel", "Processing chunk $index for sourceType: $sourceType")
//            val embedding = ragEngine.generateEmbeddingFromChunk(chunk)
//            if (embedding.isNotEmpty()) {
//                val embeddingEntity = EmbeddingEntity(
//                    chunkId = UUID.randomUUID(),
//                    chunkText = chunk,
//                    embedding = serializeEmbedding(embedding),
//                    sourceType = sourceType,
//                    sourcePage = index,
//                    createdAt = System.currentTimeMillis()
//                )
//                embeddingUtilsRepository.saveEmbedding(embeddingEntity)
//                Log.d("DriverProfileViewModel", "Saved embedding for chunk $index")
//            } else {
//                Log.e("DriverProfileViewModel", "Failed to generate embedding for chunk $index")
//            }
//        }
//        Log.d("DriverProfileViewModel", "Finished processAndStoreChunks for sourceType: $sourceType")
//    }

    fun insertDriverProfile(profileId: UUID, email: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val driverProfileCreate = DriverProfileCreate(
                driverProfileId = profileId,
                email = email,
                sync = true
            )
            val remoteResult = driverProfileApiRepository.createDriverProfile(driverProfileCreate)

            if (remoteResult is Resource.Success) {
                val localResult = runCatching {
                    val entity = DriverProfileEntity(email = email, driverProfileId = profileId, sync=true)
                    insertDriverProfileUseCase.execute(entity)
                }
                withContext(Dispatchers.Main) {
                    callback(localResult.isSuccess)
                    if (localResult.isSuccess) {
                        _driverProfileUploadSuccess.value = true
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    callback(false)
                    _driverProfileUploadSuccess.value = false
                }
            }
        }
    }


    fun getDriverProfileByEmail() {
        viewModelScope.launch {
            val emailValue = email.value.toString()
            Log.d("DriverProfileViewModel", "Getting driver profile by email: $emailValue")
            getDriverProfileByEmailUseCase.execute(emailValue)
        }
    }

    fun deleteDriverProfileByEmail() {
        viewModelScope.launch {
            val emailValue = email.value.toString()
            Log.d("DriverProfileViewModel", "Deleting driver profile by email: $emailValue")
            deleteDriverProfileByEmailUseCase.execute(emailValue)
        }
    }
}


