package com.uoa.nlgengine.presentation.viewmodel
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.uoa.nlgengine.domain.usecases.local.UnsafeBehavioursBtwnDatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
//import com.uoa.core.behaviouranalysis.UnsafeBehaviorAnalyser
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import com.uoa.dbda.domain.usecase.FetchRawSensorDataByDateUseCase
import com.uoa.dbda.domain.usecase.FetchRawSensorDataByTripIdUseCase
import com.uoa.dbda.domain.usecase.InsertUnsafeBehaviourUseCase
import com.uoa.nlgengine.domain.usecases.local.GetLastInsertedUnsafeBehaviourUseCase
import com.uoa.nlgengine.domain.usecases.local.UnsafeBehaviourByTripIdUseCase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LocalUnsafeBehavioursViewModel @Inject constructor(
    private val unsafeBehavioursBtwnDatesUseCase: UnsafeBehavioursBtwnDatesUseCase,
    private val unsafeBehaviourByTripId: UnsafeBehaviourByTripIdUseCase,
    private val getLastInsertedUnsafeBehaviourUseCase: GetLastInsertedUnsafeBehaviourUseCase): ViewModel() {
    // Add implementation here
    private val _unsafeBehaviours: MutableStateFlow<List<UnsafeBehaviourModel>> = MutableStateFlow(emptyList())
    val unsafeBehaviours: StateFlow<List<UnsafeBehaviourModel>> get() = _unsafeBehaviours

    private val _lastTripId: MutableLiveData<UUID?> = MutableLiveData()
    val lastTripId: MutableLiveData<UUID?> get() = _lastTripId

    private val _reportPeriod: MutableLiveData<Pair<LocalDate, LocalDate>> = MutableLiveData()
    val reportPeriod: MutableLiveData<Pair<LocalDate, LocalDate>> get() = _reportPeriod

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

//    fun getUnsafeBehavioursBetweenDates(startDate: LocalDate, endDate: LocalDate) {
//        _reportPeriod.value = Pair(startDate, endDate)
//        viewModelScope.launch {
//            val unsafeBehavioursModelList = unsafeBehavioursBtwnDatesUseCase.execute(startDate, endDate)
//            Log.d("LocalUnsafeBehavioursVM", "Fetched unsafe behaviours: $unsafeBehavioursModelList")
//            _unsafeBehaviours.value = unsafeBehavioursModelList
//        }
//    }

    fun getUnsafeBehavioursBetweenDates(startDate: LocalDate, endDate: LocalDate) {
        resetState()
        viewModelScope.launch {
            try {
                val unsafeBehavioursModelList =
                    unsafeBehavioursBtwnDatesUseCase.execute(startDate, endDate)
                if (unsafeBehavioursModelList.isNotEmpty()) {
                    _unsafeBehaviours.value = unsafeBehavioursModelList
                } else {
                    Log.i("LocalUnsafeBehavioursVM", "No unsafe behaviors found.")
                }
            } catch (e: Exception) {
                Log.e("LocalUnsafeBehavioursVM", "Error fetching behaviors", e)
            } finally {
                _isLoading.value = false
            }
        }
    }


//    fun getUnsafeBehaviourByTripId(tripId: UUID) {
//        resetState() // Reset state before loading new data
//        viewModelScope.launch {
//            if (true) {
//
//                try {
//                    val unsafeBehavioursListByTripId = unsafeBehaviourByTripId.execute(tripId)
//                    _unsafeBehaviours.value = unsafeBehavioursListByTripId
//                    _isLoading.value = false
//                } catch (e: Exception) {
//                    Log.e(
//                        "UnsafeBehavioursViewModel",
//                        "Error fetching analysis results by TripId",
//                        e
//                    )
//                    _isLoading.value = false
//                }
//            }
//            else{
//                _unsafeBehaviours.value = emptyList()
//                _isLoading.value = false
//
//
//                }
//        }
//    }

    fun getUnsafeBehavioursForLastTrip() {
        resetState() // Reset state before loading new data
        viewModelScope.launch {
            if (true) {

                try {
                    val lastInsertedUnsafeBehavioursTripId = getLastInsertedUnsafeBehaviourUseCase.execute()?.tripId

                    val lastInsertedUnsafeBehaviours=unsafeBehaviourByTripId.execute(lastInsertedUnsafeBehavioursTripId!!)
                    _unsafeBehaviours.value = lastInsertedUnsafeBehaviours
                    _isLoading.value = false
                } catch (e: Exception) {
                    Log.e(
                        "UnsafeBehavioursViewModel",
                        "Error fetching analysis results by TripId",
                        e
                    )
                    _isLoading.value = false
                }
            }
            else{
                _unsafeBehaviours.value = emptyList()
                _isLoading.value = false


            }
        }
    }

    // Function to reset or clear the ViewModel state
    private fun resetState() {
        _unsafeBehaviours.value = emptyList()
        _isLoading.value = true
    }


    fun fetchLastInsertedUnsafeBehaviour() {
        viewModelScope.launch {
            val tripId = getLastInsertedUnsafeBehaviourUseCase.execute()?.tripId
            _lastTripId.postValue(tripId)
        }

}
    }