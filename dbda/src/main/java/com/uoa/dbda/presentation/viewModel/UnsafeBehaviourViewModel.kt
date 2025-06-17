//package com.uoa.dbda.presentation.viewModel
//
//import android.os.Build
//import android.util.Log
//import androidx.annotation.RequiresApi
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.uoa.dbda.domain.usecase.AnalyzeUnsafeBehaviorUseCase
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.isActive
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//@HiltViewModel
//class UnsafeBehaviourViewModel @Inject constructor(
//    private val analyzeUnsafeBehaviorUseCase: AnalyzeUnsafeBehaviorUseCase,
////    private val application: Application, // Inject Application for context
////    @JvmSuppressWildcards private val log: LogFunction
//) : ViewModel() {
//
//    var analysisJob: Job? = null
//
//    fun startUnsafeBehaviorAnalysis(timeInterval: Long = 10000L) { // Default interval of 10 seconds
//        analysisJob?.cancel() // Cancel any existing analysis
//        analysisJob = viewModelScope.launch {
//            while (isActive) {
//                try {
//                    val endTime = System.currentTimeMillis()
//                    val startTime = endTime - timeInterval
//                    analyzeUnsafeBehaviorUseCase.execute(startTime, endTime)
//                } catch (e: Exception) {
//                    // Handle exceptions gracefully (log, show error message, etc.)
//                    // For example:
//                    Log.e("UnsafeBehaviourViewModel", "Error during analysis", e)
//                }
//                delay(timeInterval)
//            }
//        }
////        return analysisJob!!
//    }
//
//    fun stopUnsafeBehaviorAnalysis() {
//        analysisJob?.cancel()
//        analysisJob = null
//    }
//}
