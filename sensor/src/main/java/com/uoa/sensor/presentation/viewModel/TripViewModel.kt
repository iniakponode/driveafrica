package com.uoa.sensor.presentation.viewModel
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.sensor.data.model.Trip
import com.uoa.sensor.domain.usecases.trip.FetchTripUseCase
import com.uoa.sensor.domain.usecases.trip.InsertTripUseCase
import com.uoa.sensor.domain.usecases.trip.UpdateTripUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class TripViewModel @Inject constructor(
    private val insertTripUseCase: InsertTripUseCase,
    private val updateTripUseCase: UpdateTripUseCase,
    private val fetchTripUseCase: FetchTripUseCase
) : ViewModel() {

    private val _currentTripId = MutableStateFlow<Long?>(null)
    val currentTripId: StateFlow<Long?> get() = _currentTripId

    fun startTrip(driverProfileId: Long?) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val trip = Trip(driverProfileId = driverProfileId, startTime = startTime, endTime = null)
            val tripId = insertTripUseCase(trip)
            _currentTripId.value = tripId
        }
    }

    fun endTrip(tripId: Long) {
        viewModelScope.launch {
            _currentTripId.value?.let { tripId ->
                val trip = fetchTripUseCase.invoke(tripId)
                trip?.let {
                    it.endTime = System.currentTimeMillis()
                    updateTripUseCase(it)
                    Log.i("TripID", "Trip ended with id: ${it.id}")
                    _currentTripId.value = null
                }
            }
        }
    }
}


