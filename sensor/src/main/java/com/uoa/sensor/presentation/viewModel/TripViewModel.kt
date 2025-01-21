package com.uoa.sensor.presentation.viewModel
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.apiServices.models.tripModels.TripCreate
import com.uoa.core.apiServices.services.tripApiService.TripApiRepository
import com.uoa.core.mlclassifier.data.InferenceResult
import com.uoa.core.model.Trip
import com.uoa.core.network.Dispatcher
import com.uoa.core.utils.Resource
import com.uoa.ml.domain.RunClassificationUseCase
import com.uoa.sensor.domain.usecases.trip.GetTripByIdUseCase
import com.uoa.sensor.domain.usecases.trip.InsertTripUseCase
import com.uoa.sensor.domain.usecases.trip.UpdateTripUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val tripApiRepository: TripApiRepository


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
                synced = true
            )

            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val isoStartDate = sdf.format(Date())

            val tripCreate = TripCreate(
                id = tripId,
                driverProfileId = driverProfileId,
                start_date = isoStartDate,
                end_date = null,
                start_time = startTime,
                end_time = null,
                synced = true
            )

            // 1) Insert the trip locally first
            insertTripUseCase(trip)
            _tripUploadSuccess.value = true
            _currentTripId.value = tripId
            _currentTripId.emit(tripId)

            // 2) Now attempt remote creation
//            val uploadResult = tripApiRepository.createTrip(tripCreate)
//            if (uploadResult is Resource.Success) {
//                _tripUploadSuccess.value = true
//                Log.i("TripViewModel", "Remote trip creation succeeded for tripId: $tripId")
//            } else {
//                _tripUploadSuccess.value = false
//                Log.e("TripViewModel", "Remote trip creation failed for tripId: $tripId")
//            }
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
                        val alcInfluence = inferenceResult.alcoholInfluence
                        val influenceValue = if (alcInfluence) "alcohol" else "No influence"

                        // 2) Get current local trip info (still on IO)
                        val updatedTrip = withContext(Dispatchers.IO) {
                            getTripByIdUseCase.invoke(tripId)
                        }

                        // Prepare date/time fields (can be done on main or IO)
                        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        val isoStartDate = updatedTrip.startDate?.let { sdf.format(it) }
                        val isoEndDate = sdf.format(Date())      // Current date in ISO-8601
                        val endTime = System.currentTimeMillis() // Current time in milliseconds

                        // Create the trip payload
                        val tripCreate = TripCreate(
                            id = updatedTrip.id,
                            driverProfileId = updatedTrip.driverPId,
                            start_date = isoStartDate,
                            end_date = isoEndDate,
                            start_time = updatedTrip.startTime, // BigInteger on server
                            end_time = endTime,                 // Current time as BigInteger
                            synced = true
                        )

                        // 3) Update local data first (on IO)
                        withContext(Dispatchers.IO) {
                            updateTripUseCase.invoke(tripId, influenceValue)
                        }
                        _tripUploadSuccess.value = true
                        Log.i("TripViewModel", "Local trip updated with '$influenceValue' successfully.")

                        // 4) Attempt remote update (on IO) after local update
                        val uploadResult = withContext(Dispatchers.IO) {
                            tripApiRepository.createTrip(tripCreate)
                        }

                        if (uploadResult is Resource.Success) {
                            Log.i("TripViewModel", "Remote trip update succeeded for tripId: $tripId")
                        } else {
                            _tripUploadSuccess.value = false
                            Log.e("TripViewModel", "Remote update failed for tripId: $tripId")
                        }

                        // Log classification result after the full chain completes
                        Log.i("TripViewModel", "Alcohol Influence: $alcInfluence")
                    }

                    is InferenceResult.Failure -> {
                        val error = inferenceResult.error
                        Log.e(
                            "TripViewModel",
                            "Classification failed for tripId: $tripId with error: ${error.message}",
                            error
                        )
                    }
                }
            } catch (updateException: Exception) {
                // Catch any exception that occurs during the chain
                Log.e(
                    "TripViewModel",
                    "Failed to end trip for $tripId: ${updateException.message}",
                    updateException
                )
            }
        }
    }

}


