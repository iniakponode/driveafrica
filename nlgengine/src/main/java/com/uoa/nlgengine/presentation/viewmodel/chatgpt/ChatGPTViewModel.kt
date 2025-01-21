package com.uoa.nlgengine.presentation.viewmodel.chatgpt

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.uoa.core.nlg.repository.NLGEngineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.apiServices.services.nlgReportApiService.NLGReportApiRepository
import com.uoa.core.database.entities.NLGReportEntity
import com.uoa.core.database.repository.NLGReportRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.network.model.chatGPT.Message
import com.uoa.core.network.model.chatGPT.RequestBody
import com.uoa.core.utils.PeriodType
import com.uoa.core.utils.PeriodUtils
import com.uoa.core.utils.PeriodUtils.toJavaUtilDate
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toNLGReportCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatGPTViewModel @Inject constructor(
    private val nlgEngineRepository: NLGEngineRepository,
    private val nlgReportRepository: NLGReportRepository,
    private val nlgReportApiRepository: NLGReportApiRepository,
    private val tripDataRepository: TripDataRepository
) : ViewModel() {

    private val _response = MutableLiveData<String>("")
    val response: LiveData<String> get() = _response

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

//    fun getChatGPTPrompt(prompt: String) {
//        viewModelScope.launch {
//            _isLoading.value = true
//            try {
//                val requestBody = RequestBody(
//                    model = "gpt-3.5-turbo",
//                    messages = listOf(Message(role = "user", content = prompt)),
//                    maxTokens = 500,
//                    temperature = 0f
//                )
//
//                val response = nlgEngineRepository.sendChatGPTPrompt(requestBody)
//                val reply = response.choices.firstOrNull()?.message?.content ?: ""
//                _response.value = reply
//                _isLoading.value = false
//
//                Log.d("ChatGPTViewModel", "Response: $reply")
//            } catch (e: Exception) {
//                Log.e("ChatGPTViewModel", "Error fetching chat completion", e)
//                _response.value = "Error fetching data"
//                _isLoading.value = false
//            }
//            finally {
//                _isLoading.value = false
//            }
//        }
//    }

//    fun getChatGPTPrompt(prompt: String) {
//        viewModelScope.launch {
//            _isLoading.value = true
//            try {
//                val systemMessageContent = """
//            You are an assistant that generates driving behavior reports based on provided data. Your report should include the key components of the Theory of Planned Behavior (TPB) and adhere strictly to the provided data. Before presenting the report, reflect on it to ensure it meets all specified criteria. If any components are missing or need correction, revise your report accordingly. Present only the final, corrected report to the user without including any reflection or internal notes.
//            """.trimIndent()
//
//                val messages = listOf(
//                    Message(role = "system", content = systemMessageContent),
//                    Message(role = "user", content = prompt)
//                )
//
//                val requestBody = RequestBody(
//                    model = "gpt-3.5-turbo",
//                    messages = messages,
//                    maxTokens = 200, // Adjust as needed
//                    temperature = 0f
//                )
//
//                val response = nlgEngineRepository.sendChatGPTPrompt(requestBody)
//                val finalReply = response.choices.firstOrNull()?.message?.content ?: ""
//                _response.value = finalReply
//                _isLoading.value = false
//
//                Log.d("ChatGPTViewModel", "Response: $finalReply")
//            } catch (e: Exception) {
//                Log.e("ChatGPTViewModel", "Error fetching chat completion", e)
//                _response.value = "Error fetching data"
//                _isLoading.value = false
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }

    suspend fun getCachedReport(periodType: PeriodType): String?{
        if (periodType== PeriodType.LAST_TRIP){
            val tripId= tripDataRepository.getLastInsertedTrip()?.id

            if (tripId!=null){
                val cachedNlgReport=nlgReportRepository.getNlgReportsByTripId(tripId)

                return cachedNlgReport.reportText
            }
            else{
                return null
            }
        }
        else{
            val periodOfReport=PeriodUtils.getReportingPeriod(periodType)

            if(periodOfReport!=null){
                val cachedNlgReport=nlgReportRepository.getReportsBetweenDates(periodOfReport.first,periodOfReport.second)

                return cachedNlgReport.reportText
            }
            else{
                return null
            }
        }
    }
    fun promptChatGPTForResponse(prompt: String, periodType: PeriodType, driverProfileId: UUID) {
        viewModelScope.launch {
            _isLoading.value = true
        try {
            if (getCachedReport(periodType) == null){
                val systemMessageContent = """
            You are an assistant that generates driving behavior reports based on provided data. After generating the report, you will reflect on your response to ensure it includes the key components of the Theory of Planned Behavior (TPB) and adheres strictly to the provided data.
            """.trimIndent()

                val messages = mutableListOf(
                    Message(role = "system", content = systemMessageContent),
                    Message(role = "user", content = prompt)
                )

                // Step 1: Get the initial report
                val requestBody = RequestBody(
                    model = "gpt-4-turbo",
                    messages = messages,
                    maxTokens = 200,
                    temperature = 0f
                )

                val response = nlgEngineRepository.sendChatGPTPrompt(requestBody)
                val initialReply = response.choices.firstOrNull()?.message?.content ?: ""

                // Add the assistant's response to the conversation
                messages.add(Message(role = "assistant", content = initialReply))

                // Step 2: Ask for reflection and revised report
                messages.add(
                    Message(
                        role = "user", content = """
                Please reflect on your previous response to ensure it meets all specified criteria,
                including the key components of the Theory of Planned Behavior (TPB). 
                If any components are missing or need correction, 
                provide a revised report that includes all necessary elements. 
                Present only the final, corrected report to the user without including any reflection or internal notes.
                Do not also mention the TPB keywords (Attitudes, Subjective Norms and Perceived Behavioral Control) in the report, other descriptive words should be used that drivers may easily understand if there is ever the need to mention those words.
            """.trimIndent()
                    )
                )

                val reflectionRequestBody = RequestBody(
                    model = "gpt-4-turbo",
                    messages = messages,
                    maxTokens = 300,
                    temperature = 0f
                )

                val reflectionResponse =
                    nlgEngineRepository.sendChatGPTPrompt(reflectionRequestBody)
                val finalReply =
                    reflectionResponse.choices.firstOrNull()?.message?.content ?: ""

                // Assign the final reply to the response
                _response.value = finalReply
                _isLoading.value = false

                if (periodType== PeriodType.LAST_TRIP){

                    val lastTripId=tripDataRepository.getLastInsertedTrip()?.id
                nlgReportRepository.insertReport(
                    NLGReportEntity(
                        id = UUID.randomUUID(),
                        userId =driverProfileId,
                        tripId = lastTripId,
                        reportText = finalReply,
                        createdDate = PeriodUtils.getReportingPeriod(PeriodType.TODAY)!!.first,
                        synced = false
                    )
                )

                nlgReportApiRepository.createNLGReport(
                        NLGReportEntity(
                            id = UUID.randomUUID(),
                            userId =driverProfileId,
                            startDate = PeriodUtils.getReportingPeriod(periodType)?.first!!,
                            endDate = PeriodUtils.getReportingPeriod(periodType)?.second!!,
                            reportText = finalReply,
                            createdDate = PeriodUtils.getReportingPeriod(PeriodType.TODAY)!!.first,
                            synced = true
                        ).toDomainModel().toNLGReportCreate()
                    )

                }
                else{
                    nlgReportRepository.insertReport(
                        NLGReportEntity(
                            id = UUID.randomUUID(),
                            userId =driverProfileId,
                            startDate = PeriodUtils.getReportingPeriod(periodType)?.first!!,
                            endDate = PeriodUtils.getReportingPeriod(periodType)?.second!!,
                            reportText = finalReply,
                            createdDate = PeriodUtils.getReportingPeriod(PeriodType.TODAY)!!.first,
                            synced = true
                        )
                    )

                    nlgReportApiRepository.createNLGReport(
                        NLGReportEntity(
                            id = UUID.randomUUID(),
                            userId =driverProfileId,
                            startDate = PeriodUtils.getReportingPeriod(periodType)?.first!!,
                            endDate = PeriodUtils.getReportingPeriod(periodType)?.second!!,
                            reportText = finalReply,
                            createdDate = PeriodUtils.getReportingPeriod(PeriodType.TODAY)!!.first,
                            synced = false
                        ).toDomainModel().toNLGReportCreate()
                    )
                }

                // Optionally, log both initial and final replies
                initialReply.chunked(100).forEach {
                    Log.d("ChatGPTViewModel", "Initial Response: $it")
                }
                finalReply.chunked(100).forEach {
                    Log.d("ChatGPTViewModel", "Final Response: $it")
                }
//                Log.d("ChatGPTViewModel", "Final Response: $finalReply")
            }
            else{
                val resp=getCachedReport(periodType)
                _response.value = resp!!
                _isLoading.value = false
            }
                } catch (e: Exception) {
                    Log.e("ChatGPTViewModel", "Error fetching chat completion", e)
                    _response.value = "Error fetching data"
                    _isLoading.value = false
                } finally {
                    _isLoading.value = false
                }
        }
    }


}

