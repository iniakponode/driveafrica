package com.uoa.sensor.presentation.viewModel
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.apiServices.models.tripModels.TripCreate
import com.uoa.core.apiServices.models.tripModels.TripResponse
import com.uoa.core.apiServices.services.tripApiService.TripApiRepository
import com.uoa.core.database.repository.DriverProfileRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.mlclassifier.data.InferenceResult
import com.uoa.core.model.Trip
import com.uoa.core.notifications.VehicleNotificationManager
import com.uoa.core.utils.Resource
import com.uoa.core.utils.isConnectedToInternet
import com.uoa.core.utils.toTrip
import com.uoa.ml.domain.RunClassificationUseCase
import com.uoa.sensor.domain.usecases.trip.GetTripByIdUseCase
import com.uoa.sensor.domain.usecases.trip.InsertTripUseCase
import com.uoa.sensor.domain.usecases.trip.UpdateTripUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TripViewModel @Inject constructor(
    private val insertTripUseCase: InsertTripUseCase,
    private val updateTripUseCase: UpdateTripUseCase,
    private val getTripByIdUseCase: GetTripByIdUseCase,
    private val runClassificationUseCase: RunClassificationUseCase,
    private val tripApiRepository: TripApiRepository,
    private val localTripRepository: TripDataRepository


) : ViewModel() {

    private val _currentTripId = MutableStateFlow<UUID?>(null)
    val currentTripId: StateFlow<UUID?> get() = _currentTripId

    private val _tripUploadSuccess = MutableStateFlow(false)
    val tripUploadSuccess: StateFlow<Boolean> get() = _tripUploadSuccess

    fun startTrip(driverProfileId: UUID?, tripId: UUID) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val trip = Trip(
                driverPId = driverProfileId,
                startTime = startTime,
                endTime = null,
                startDate = Date(),
                endDate = null,
                id = tripId,
                influence = "",
                sync = true
            )

            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val isoStartDate = sdf.format(Date())

            val tripCreate = TripCreate(
                id = tripId,
                driverProfileId = driverProfileId,
                startDate = isoStartDate,
                endDate = null,
                startTime = startTime,
                endTime = null,
                influence = "",
                sync = true
            )

            // 1) Insert the trip locally first
            insertTripUseCase(trip)
            _tripUploadSuccess.value = true
            _currentTripId.value = tripId
            _currentTripId.emit(tripId)

            // 2) Now attempt remote creation
            val uploadResult = tripApiRepository.createTrip(tripCreate)
            if (uploadResult is Resource.Success) {
                _tripUploadSuccess.value = true
                Log.i("TripViewModel", "Remote trip creation succeeded for tripId: $tripId")
            } else {
                _tripUploadSuccess.value = false
                Log.e("TripViewModel", "Remote trip creation failed for tripId: $tripId")
            }
        }
    }

    fun updateTripId(tripId: UUID) {
        _currentTripId.value = tripId
    }

    fun clearTripID() {
        _currentTripId.value = null
    }


    /**
     * Ends a trip by performing classification and updating trip status based on alcohol influence.
     *
     * @param tripId The UUID of the trip to end.
     */
    fun endTrip(tripId: UUID) {
        viewModelScope.launch {
            Log.i("TripViewModel", "endTrip called for tripId: $tripId")

            try {
                // 1) Run classification in IO
                val inferenceResult = withContext(Dispatchers.IO) {
                    runClassificationUseCase.invoke(tripId)
                }

                when (inferenceResult) {
                    is InferenceResult.Success -> {
                        Log.i("TripViewModel", "Got into successful inference")
                        // Decide whether it's "alcohol" or "No influence"
                        val alcInfluence = inferenceResult.isAlcoholInfluenced
                        val influenceLabel = if (alcInfluence) "alcohol" else "No influence"

                        // Update local + remote with final classification
                        val uploadSucceeded = finalizeTripClassification(
                            tripId = tripId,
                            influenceLabel = influenceLabel,
                            getTripByIdUseCase = getTripByIdUseCase,
                            updateTripUseCase = updateTripUseCase,
                            tripApiRepository = tripApiRepository,
                            localTripRepository = localTripRepository,
                            tripUploadSuccess = _tripUploadSuccess
                        )

                        // Additional logic if needed
                        Log.i("TripViewModel", "Inference result was: $influenceLabel. Upload success? $uploadSucceeded")
                    }

                    is InferenceResult.NotEnoughData -> {
                        Log.w("TripViewModel", "Not enough data for model to classify tripId: $tripId")

                        // If you still want to finalize & upload with a special label
                        val influenceLabel = "Not enough data for model classification"

                        val uploadSucceeded = finalizeTripClassification(
                            tripId = tripId,
                            influenceLabel = influenceLabel,
                            getTripByIdUseCase = getTripByIdUseCase,
                            updateTripUseCase = updateTripUseCase,
                            tripApiRepository = tripApiRepository,
                            localTripRepository = localTripRepository,
                            tripUploadSuccess = _tripUploadSuccess
                        )
                        Log.i("TripViewModel", "Finalized trip with label=$influenceLabel. Upload success? $uploadSucceeded")
                    }

                    is InferenceResult.Failure -> {
                        val error = inferenceResult.error
                        Log.e(
                            "TripViewModel",
                            "Classification failed for tripId: $tripId with error: ${error.message}",
                            error
                        )

                        // If you want to finalize & upload with a special label
                        val influenceLabel = "Not enough data for model classification"

                        val uploadSucceeded = finalizeTripClassification(
                            tripId = tripId,
                            influenceLabel = influenceLabel,
                            getTripByIdUseCase = getTripByIdUseCase,
                            updateTripUseCase = updateTripUseCase,
                            tripApiRepository = tripApiRepository,
                            localTripRepository = localTripRepository,
                            tripUploadSuccess = _tripUploadSuccess
                        )
                        Log.i("TripViewModel", "Finalized trip with label=$influenceLabel. Upload success? $uploadSucceeded")
                    }
                }
            } catch (updateException: Exception) {
                // Catch any exception that occurs during the chain
                Log.e(
                    "TripViewModel",
                    "Failed to end trip for $tripId: ${updateException.message}",
                    updateException
                )
                _tripUploadSuccess.value = false
            }
        }
    }


    suspend fun finalizeTripClassification(
        tripId: UUID,
        influenceLabel: String, // "alcohol", "No influence", or "Not enough data for model classification"
        getTripByIdUseCase: GetTripByIdUseCase,
        updateTripUseCase: UpdateTripUseCase,
        tripApiRepository: TripApiRepository,
        localTripRepository: TripDataRepository,
        tripUploadSuccess: MutableStateFlow<Boolean>
    ): Boolean {
        // 1) Fetch current local trip info (still on IO)
        val updatedTrip = withContext(Dispatchers.IO) {
            getTripByIdUseCase.invoke(tripId)
        }

        // Prepare date/time fields. Example: UTC+1 zone
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC+1")
        }
        val isoStartDate = updatedTrip.startDate?.let { sdf.format(it) }
        val isoEndDate = sdf.format(Date()) // Current date in ISO-8601
        val endTime = System.currentTimeMillis() // Current time in ms

        // 2) Build the TripCreate payload, setting the `influence` to our label
        val tripCreate = TripCreate(
            id = updatedTrip.id,
            driverProfileId = updatedTrip.driverPId,
            startDate = isoStartDate,
            endDate = isoEndDate,
            startTime = updatedTrip.startTime, // BigInteger on server
            endTime = endTime,                 // Current time as BigInteger
            influence = influenceLabel,
            sync = true
        )

        // 3) Update local data first (on IO)
        withContext(Dispatchers.IO) {
            updateTripUseCase.invoke(tripId, influenceLabel)
            Log.i("TripViewModel", "Locally updated trip with '$influenceLabel'.")
        }

        tripUploadSuccess.value = true
        Log.i("TripViewModel", "Local trip updated successfully with '$influenceLabel'.")

        // 4) Attempt remote update (on IO) after local update
        val uploadResult = withContext(Dispatchers.IO) {
            tripApiRepository.updateTrip(updatedTrip.id, tripCreate)
        }

        // 5) Check remote upload success/failure
        return if (uploadResult is Resource.Success) {
            localTripRepository.updateUploadStatus(updatedTrip.id, true)
            Log.i("TripViewModel", "Remote trip update succeeded for tripId: $tripId")
            true
        } else {
            tripUploadSuccess.value = false
            Log.e("TripViewModel", "Remote update failed for tripId: $tripId")
            false
        }
    }

    }


