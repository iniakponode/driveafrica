package com.uoa.sensor.presentation.viewModel
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.model.Trip
import com.uoa.sensor.domain.usecases.trip.FetchTripUseCase
import com.uoa.sensor.domain.usecases.trip.InsertTripUseCase
import com.uoa.sensor.domain.usecases.trip.UpdateTripUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject
@HiltViewModel
class TripViewModel @Inject constructor(
    private val insertTripUseCase: InsertTripUseCase,
    private val updateTripUseCase: UpdateTripUseCase,
    private val fetchTripUseCase: FetchTripUseCase
) : ViewModel() {

    private val _currentTripId = MutableStateFlow<UUID?>(null)
    val currentTripId: StateFlow<UUID?> get() = _currentTripId

    fun startTrip(driverProfileId: UUID?, tripId: UUID){
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val trip = Trip(driverPId = driverProfileId,
                startTime = startTime, endTime = null,
                startDate = Date(),
                endDate = null,
                id = tripId,
                influence = ""
            )
            insertTripUseCase(trip)
            _currentTripId.value = tripId
            _currentTripId.emit(tripId)
            Log.i("TripID", "Trip started with id: $tripId")
        }
    }

    fun updateTripId(tripId: UUID): UUID{
        viewModelScope.launch {
            _currentTripId.emit(tripId)
        }
        return tripId
    }

//    fun endTrip(tripId: UUID) {
//        viewModelScope.launch {
//                    updateTripUseCase(tripId)
////
//                    Log.i("TripID", "Trip ended with id: ${currentTripId.value}")
//                }
//            }
}


