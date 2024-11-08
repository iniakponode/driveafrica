package com.uoa.ml.presentation.viewmodel

import android.util.Log
import com.uoa.ml.domain.RunClassificationUseCase
import com.uoa.ml.domain.UpDateUnsafeBehaviourCauseUseCase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.ml.domain.SaveInfluenceToCause
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
@HiltViewModel
class AlcoholInfluenceViewModel @Inject constructor(
    private val runClassificationUseCase: RunClassificationUseCase,
    private val upDateUnsafeBehaviourCauseUseCase: UpDateUnsafeBehaviourCauseUseCase,
    private val saveInfluenceToCause: SaveInfluenceToCause
) : ViewModel() {

    private val _alcoholInfluence = MutableLiveData(false)
    val alcoholInfluence: LiveData<Boolean> get() = _alcoholInfluence

    fun classifySaveAndUpdateUnsafeBehaviour(tripId: UUID) {
        viewModelScope.launch{
            val influence = runClassificationUseCase.invoke(tripId)
            Log.d("AlcoholIn", "Influence: $influence")
            _alcoholInfluence.value = influence
            upDateUnsafeBehaviourCauseUseCase.invoke(tripId, influence)
        }

    }

    fun saveInfluenceToCauseTable(tripId: UUID){
        viewModelScope.launch{
            val influence = runClassificationUseCase.invoke(tripId)
            Log.d("AlcoholIn", "Influence: $influence")
            saveInfluenceToCause.invoke(tripId, influence)
            Log.d("AlcoholIn", "Influence saved to cause table")
//            return influence

        }

    }
}