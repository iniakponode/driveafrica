package com.uoa.nlgengine.presentation.viewmodel.chatgpt

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.uoa.core.nlg.lngrepositoryimpl.NLGEngineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.network.model.chatGPT.Message
import com.uoa.core.network.model.chatGPT.RequestBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatGPTViewModel @Inject constructor(
    private val nlgEngineRepository: NLGEngineRepository
) : ViewModel() {

    private val _response = MutableLiveData<String>("")
    val response: LiveData<String> get() = _response

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    fun getChatGPTPrompt(prompt: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val requestBody = RequestBody(
                    model = "gpt-3.5-turbo",
                    messages = listOf(Message(role = "user", content = prompt)),
                    maxTokens = 200,
                    temperature = 0f
                )

                val response = nlgEngineRepository.sendChatGPTPrompt(requestBody)
                val reply = response.choices.firstOrNull()?.message?.content ?: ""
                _response.value = reply
                _isLoading.value = false

                Log.d("ChatGPTViewModel", "Response: $reply")
            } catch (e: Exception) {
                Log.e("ChatGPTViewModel", "Error fetching chat completion", e)
                _response.value = "Error fetching data"
                _isLoading.value = false
            }
            finally {
                _isLoading.value = false
            }
        }
    }
}

