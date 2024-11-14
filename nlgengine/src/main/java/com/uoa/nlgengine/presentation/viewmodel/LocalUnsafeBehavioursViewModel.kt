package com.uoa.nlgengine.presentation.viewmodel
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.uoa.nlgengine.domain.usecases.local.UnsafeBehavioursBtwnDatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.behaviouranalysis.UnsafeBehaviorAnalyser
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
class LocalUnsafeBehavioursViewModel @Inject constructor(private val unsafeBehavioursBtwnDatesUseCase: UnsafeBehavioursBtwnDatesUseCase,
                                                         private val unsafeBehaviourByTripId: UnsafeBehaviourByTripIdUseCase,
                                                         private val fetchRawSensorDataByDateUsecase: FetchRawSensorDataByDateUseCase,
                                                         private val fetchRawSensorDataByTripIdUseCase: FetchRawSensorDataByTripIdUseCase,
                                                         private val unsafeBehaviourAnalyser: UnsafeBehaviorAnalyser,
                                                         private val insertUnsafeBehaviourUseCase: InsertUnsafeBehaviourUseCase,
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
        resetState() // Reset state before loading new data
        _reportPeriod.value = Pair(startDate, endDate)
        viewModelScope.launch {
            // Reset the state before fetching new data
            _unsafeBehaviours.value = emptyList()
            val unsafeBehavioursModelList = unsafeBehavioursBtwnDatesUseCase.execute(startDate, endDate)
//            Log.d("LocalUnsafeBehavioursVM", "Fetched unsafe behaviours: $unsafeBehavioursModelList")
            if (unsafeBehavioursModelList.isNotEmpty()){

                unsafeBehavioursModelList.chunked(100).forEach { chunk ->
//                    Log.d("LocalUnsafeBehavioursVM", "Unsafe Behaviours between $startDate and $endDate: $chunk")
                }
                _unsafeBehaviours.value = unsafeBehavioursModelList
            }
            else{
                try {

                    val rawSensorDataList = fetchRawSensorDataByDateUsecase.execute(startDate, endDate).first()
                    val res = rawSensorDataList.map { it.toEntity() }
                    Log.d("AnalysisViewModel", "Number of sensor data by Date: ${res.size}, $startDate, $endDate")
                    val unsafeBehaviour = unsafeBehaviourAnalyser.analyse(res)
                    if (unsafeBehaviour.isEmpty()) {
                        Log.d("AnalysisViewModel", "Number of unsafe behaviours by Date is empty.")
                    }
                    else {
                        Log.d("AnalysisViewModel", "Number of unsafe behaviours by Date: ${unsafeBehaviour.size}")
                        unsafeBehaviour.map { insertUnsafeBehaviourUseCase.execute(it) }
                    }
                    _unsafeBehaviours.value=unsafeBehaviour

                } catch (e: Exception) {
                    Log.e("AnalysisViewModel", "Error fetching analysis results by date", e)
                }
            }

            _isLoading.value = false
        }
    }


    fun getUnsafeBehaviourByTripId() {
        resetState() // Reset state before loading new data
        viewModelScope.launch {
            val trip = getLastInsertedUnsafeBehaviourUseCase.execute()
            val tripId = trip?.tripId
            if (tripId != null) {
                val unsafeBehavioursListByTripId = unsafeBehaviourByTripId.execute(tripId)

                if (unsafeBehavioursListByTripId.isNotEmpty()) {


                    unsafeBehavioursListByTripId.take(5).forEach { chunk ->
//                        Log.d(
//                            "LocalUnsafeBehavioursVM",
//                            "Unsafe Behaviours for trip $tripId: $chunk"
//                        )
                    }
//                Log.d("LocalUnsafeBehavioursVM", "Fetched unsafe behaviours by trip ID: $unsafeBehavioursListByTripId")
                    _unsafeBehaviours.value = unsafeBehavioursListByTripId
                    _isLoading.value = false
                }
                else{
                    try {

                        val rawSensorDataList = fetchRawSensorDataByTripIdUseCase.execute(tripId).first()
                        val res = rawSensorDataList.map { it.toEntity() }
                        Log.d("AnalysisViewModel", "Number of sensor data by ID: ${res.size}, $tripId")
                        val unsafeBehaviour = unsafeBehaviourAnalyser.analyse(res)
                        if (unsafeBehaviour.isEmpty()) {
                            Log.d("AnalysisViewModel", "Number of unsafe behaviours by Trip ID is empty.")
                        }
                        else {
                            Log.d("AnalysisViewModel", "Number of unsafe behaviours by Trip ID: ${unsafeBehaviour.size}")
                            unsafeBehaviour.map { insertUnsafeBehaviourUseCase.execute(it) }
                        }
                        _unsafeBehaviours.value=unsafeBehaviour
                    } catch (e: Exception) {
                        Log.e("AnalysisViewModel", "Error fetching analysis results by Trip ID", e)
                    }
                }
            } else {
                Log.d("LocalUnsafeBehavioursVM", "No tripId found")
                _isLoading.value = false
            }
        }
    }

    // Function to reset or clear the ViewModel state
    private fun resetState() {
        _unsafeBehaviours.value = emptyList()
        _isLoading.value = true
    }

//    fun fetchLastInsertedUnsafeBehaviour() {
//        viewModelScope.launch {
//            val tripId = getLastInsertedUnsafeBehaviourUseCase.execute()?.tripId
//            _lastTripId.postValue(tripId)
//        }
//
//}
    }