package com.uoa.dbda.presentation.viewModel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
//import com.uoa.core.behaviouranalysis.UnsafeBehaviorAnalyser
import com.uoa.core.model.RawSensorData
import com.uoa.dbda.domain.usecase.FetchRawSensorDataByDateUseCase
import com.uoa.dbda.domain.usecase.FetchRawSensorDataByTripIdUseCase
import com.uoa.dbda.domain.usecase.InsertUnsafeBehaviourUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val fetchRawSensorDataByDateUseCase: FetchRawSensorDataByDateUseCase,
    private val fetchRawSensorDataByTripIdUseCase: FetchRawSensorDataByTripIdUseCase,
    private val insertUnsafeBehaviourUseCase: InsertUnsafeBehaviourUseCase,
//    private val unsafeBehaviorAnalyser: UnsafeBehaviorAnalyser
) : ViewModel() {

    private val _analysisResult = MutableLiveData<List<RawSensorData>>()
    val analysisResult: LiveData<List<RawSensorData>> get() = _analysisResult


//    fun analyseUnsafeBehaviourByTrip(tripId: UUID) {
//        viewModelScope.launch {
//            try {
//                val rawSensorDataList = fetchRawSensorDataByTripIdUseCase.execute(tripId).first()
//                val res = rawSensorDataList.map { it.toEntity() }
//                Log.i("Analysis", "Number of sensor data by Trip Id: ${res.size}")
//                val unsafeBehaviour = unsafeBehaviorAnalyser.analyse(res)
//                if (unsafeBehaviour.isEmpty()) {
//                    Log.i("AnalysisViewModel", "Number of unsafe behaviours by Trip Id is empty.")
//                } else {
//                    Log.i("AnalysisViewModel", "Number of unsafe behaviours by Trip Id: ${unsafeBehaviour.size}")
//                    unsafeBehaviour.map { insertUnsafeBehaviourUseCase.execute(it) }
//                }
//                _analysisResult.postValue(res.map { it.toDomainModel() })
//            } catch (e: Exception) {
//                Log.e("Analysis", "Error fetching analysis results by trip", e)
//            }
//        }
//    }

//    fun analyseUnsafeBehaviourByDate(startDate: LocalDate, endDate: LocalDate) {
//        viewModelScope.launch {
//            try {
//                val rawSensorDataList = fetchRawSensorDataByDateUseCase.execute(startDate, endDate).first()
//                val res = rawSensorDataList.map { it.toEntity() }
//                Log.d("AnalysisViewModel", "Number of sensor data by Date: ${res.size}, $startDate, $endDate")
//                val unsafeBehaviour = unsafeBehaviorAnalyser.analyse(res)
//                if (unsafeBehaviour.isEmpty()) {
//                    Log.d("AnalysisViewModel", "Number of unsafe behaviours by Date is empty.")
//                }
//                else {
//                    Log.d("AnalysisViewModel", "Number of unsafe behaviours by Date: ${unsafeBehaviour.size}")
//                    unsafeBehaviour.map { insertUnsafeBehaviourUseCase.execute(it) }
//                }
//                _analysisResult.postValue(res.map { it.toDomainModel() })
//            } catch (e: Exception) {
//                Log.e("AnalysisViewModel", "Error fetching analysis results by date", e)
//            }
//        }
//    }


}