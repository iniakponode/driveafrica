package com.uoa.nlgengine.presentation.viewmodel.gemini

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.nlg.lngrepositoryimpl.NLGEngineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeminiViewModel @Inject constructor(
    private val nlgEngineRepository: NLGEngineRepository
) : ViewModel() {

    private val _response = MutableLiveData<String>()
    val response: LiveData<String> get() = _response

    fun generateText(context: Context, prompt: String) {
        viewModelScope.launch {
            try {
                val result = nlgEngineRepository.sendGeminiPrompt(context, prompt)
                _response.value = result.data.firstOrNull()?.value ?: ""
            } catch (e: Exception) {
                Log.e("GeminiViewModel", "Error generating text", e)
                _response.value = "Error generating text: ${e.message}"
            }
        }
    }
}
