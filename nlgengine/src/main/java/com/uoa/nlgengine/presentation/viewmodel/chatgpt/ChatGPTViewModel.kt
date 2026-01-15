package com.uoa.nlgengine.presentation.viewmodel.chatgpt

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.apiServices.services.nlgReportApiService.NLGReportApiRepository
import com.uoa.core.database.entities.NLGReportEntity
import com.uoa.core.database.repository.NLGReportRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.network.model.chatGPT.Message
import com.uoa.core.network.model.chatGPT.RequestBody
import com.uoa.core.nlg.repository.NLGEngineRepository
import com.uoa.core.notifications.VehicleNotificationManager
import com.uoa.core.utils.PeriodType
import com.uoa.core.utils.Resource
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toNLGReportCreate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatGPTViewModel @Inject constructor(
    private val nlgEngineRepository: NLGEngineRepository,
    private val nlgReportRepository: NLGReportRepository,
    private val nlgReportApiRepository: NLGReportApiRepository,
    private val tripDataRepository: TripDataRepository,
    application: Application
) : ViewModel() {
    private val notificationManager = VehicleNotificationManager(application.applicationContext)

    private val _response = MutableLiveData<String>("")
    val response: LiveData<String> get() = _response

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    /**
     * Retrieves a cached NLG report for the given period.
     * Returns the reportText if available; otherwise, returns null.
     */
    suspend fun getCachedReport(
        periodType: PeriodType,
        driverProfileId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): String? {
        return if (periodType == PeriodType.LAST_TRIP) {
            val tripId = tripDataRepository.getLastInsertedTrip()?.id
            if (tripId != null) {
                // Safely return reportText if the entity is not null
                nlgReportRepository.getNlgReportsByTripId(tripId)?.reportText
            } else {
                null
            }
        } else {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atStartOfDay()
            nlgReportRepository.getReportsBetweenDates(driverProfileId, startDateTime, endDateTime)?.reportText
        }
    }

    fun clearResponse() {
        _response.value = ""
    }

//    /**
//     * Generates a new chat response via ChatGPT if a cached report is not found,
//     * and saves the generated report both locally and via the API.
//     */
//    fun promptChatGPTForResponse(prompt: String, periodType: PeriodType, driverProfileId: UUID) {
//        viewModelScope.launch {
//            _isLoading.value = true
//            try {
//                // Retrieve cached report once and store in a variable.
//                val cachedReport = getCachedReport(periodType)
//                if (cachedReport == null) {
//                    val systemMessageContent = """
//                        You are an assistant that generates driving behavior reports based on provided data. After generating the report, you will reflect on your response to ensure it includes the key components of the Theory of Planned Behavior (TPB) and adheres strictly to the provided data.
//                    """.trimIndent()
//
//                    val messages = mutableListOf(
//                        Message(role = "system", content = systemMessageContent),
//                        Message(role = "user", content = prompt)
//                    )
//
//                    // Step 1: Get the initial report
//                    val requestBody = RequestBody(
//                        model = "gpt-4-turbo",
//                        messages = messages,
//                        maxTokens = 200,
//                        temperature = 0f
//                    )
//
//                    val response = nlgEngineRepository.sendChatGPTPrompt(requestBody)
//                    val initialReply = response.choices.firstOrNull()?.message?.content ?: ""
//
//                    // Add the assistant's response to the conversation history.
//                    messages.add(Message(role = "assistant", content = initialReply))
//
//                    // Step 2: Ask for reflection and revision.
//                    messages.add(
//                        Message(
//                            role = "user",
//                            content = """
//                                Please reflect on your previous response to ensure it meets all specified criteria,
//                                including the key components of the Theory of Planned Behavior (TPB).
//                                If any components are missing or need correction,
//                                provide a revised report that includes all necessary elements.
//                                Present only the final, corrected report to the user without including any reflection or internal notes.
//                                Do not mention the TPB keywords (Attitudes, Subjective Norms, and Perceived Behavioral Control) directly; use descriptive language that drivers can easily understand.
//                            """.trimIndent()
//                        )
//                    )
//
//                    val reflectionRequestBody = RequestBody(
//                        model = "gpt-4o-mini",
//                        messages = messages,
//                        maxTokens = 300,
//                        temperature = 0f
//                    )
//
//                    val reflectionResponse = nlgEngineRepository.sendChatGPTPrompt(reflectionRequestBody)
//                    val finalReply = reflectionResponse.choices.firstOrNull()?.message?.content ?: ""
//
//                    // Set the final reply as the response.
//                    _response.value = finalReply
//
//                    val lastTrip = tripDataRepository.getLastInsertedTrip()
//                    if (lastTrip == null) {
//                        Log.e("ChatGPTViewModel", "No last trip found; cannot insert report.")
//                        return@launch
//                    }
//                    val lastTripId = lastTrip.id
//                    Log.e("ChatGPTViewModel", "LastTrip ID: $lastTripId")
//
//                    // Convert dates to LocalDateTime (atStartOfDay) to support time fields in formatting.
//                    val todayPeriod = PeriodUtils.getReportingPeriod(PeriodType.TODAY)
//                    val createdDateTime: LocalDateTime = (todayPeriod?.first ?: LocalDate.now()).atStartOfDay()
//
//                    val period = PeriodUtils.getReportingPeriod(periodType)
//                    val startDateTime: LocalDateTime = (period?.first ?: LocalDate.now()).atStartOfDay()
//                    val endDateTime: LocalDateTime = (period?.second ?: LocalDate.now()).atStartOfDay()
//
//                    // Save the new report locally and remotely.
//                    if (periodType == PeriodType.LAST_TRIP) {
//                        nlgReportRepository.insertReport(
//                            NLGReportEntity(
//                                id = UUID.randomUUID(),
//                                userId = driverProfileId,
//                                tripId = lastTripId,
//                                reportText = finalReply,
//                                createdDate = createdDateTime,  // Use LocalDateTime here
//                                sync = false
//                            )
//                        )
//                        nlgReportApiRepository.createNLGReport(
//                            NLGReportEntity(
//                                id = UUID.randomUUID(),
//                                userId = driverProfileId,
//                                tripId = lastTripId,
//                                startDate = startDateTime,
//                                endDate = endDateTime,
//                                reportText = finalReply,
//                                createdDate = createdDateTime,
//                                sync = true
//                            ).toDomainModel().toNLGReportCreate()
//                        )
//                    } else {
//                        nlgReportRepository.insertReport(
//                            NLGReportEntity(
//                                id = UUID.randomUUID(),
//                                userId = driverProfileId,
//                                tripId = lastTripId,
//                                startDate = startDateTime,
//                                endDate = endDateTime,
//                                reportText = finalReply,
//                                createdDate = createdDateTime,
//                                sync = true
//                            )
//                        )
//                        val nlgReportCreate = NLGReportEntity(
//                            id = UUID.randomUUID(),
//                            userId = driverProfileId,
//                            tripId = lastTripId,
//                            startDate = startDateTime,
//                            endDate = endDateTime,
//                            reportText = finalReply,
//                            createdDate = createdDateTime,
//                            sync = false
//                        ).toDomainModel().toNLGReportCreate()
//                        val parsedDate = dateToLocalDate(stringToDate(nlgReportCreate.generated_at)!!)
//                        nlgReportApiRepository.createNLGReport(
//                            nlgReportCreate.copy(generated_at = formatDateToUTCPlusOne(parsedDate))
//                        )
//                    }
//
//                    // Log the responses in manageable chunks.
//                    initialReply.chunked(100).forEach { Log.d("ChatGPTViewModel", "Initial Response: $it") }
//                    finalReply.chunked(100).forEach { Log.d("ChatGPTViewModel", "Final Response: $it") }
//                } else {
//                    // Use the cached report if available.
//                    val cachedReport = getCachedReport(periodType)
//                    _response.value = cachedReport ?: "No cached report available."
//                    _isLoading.value = false
//                }
//            } catch (e: Exception) {
//                Log.e("ChatGPTViewModel", "Error fetching chat completion", e)
//                _response.value = "Error fetching data"
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
 fun promptChatGPTForResponse(
     prompt: String,
     periodType: PeriodType,
     driverProfileId: UUID,
     startDate: LocalDate,
     endDate: LocalDate
 ) {
     viewModelScope.launch {
         _isLoading.value = true
         try {
             val cachedReport = withContext(Dispatchers.IO) {
                getCachedReport(periodType, driverProfileId, startDate, endDate)
             }
             if (cachedReport == null) {
                val systemMessageContent = "You are an assistant generating driving behavior reports based on provided data. Ensure strict adherence to given information."

                val messages = mutableListOf(
                    Message(role = "system", content = systemMessageContent),
                    Message(role = "user", content = prompt)
                )

                val requestBody = RequestBody(
                    model = "gpt-4-turbo",
                    messages = messages,
                    maxTokens = 350,
                    temperature = 0f
                )

                val response = nlgEngineRepository.sendChatGPTPrompt(requestBody)
                val initialReply = response.choices.firstOrNull()?.message?.content ?: ""

                // Reflection step ensures final reply is coherent and complete
                messages.add(Message(role = "assistant", content = initialReply))
                messages.add(
                    Message(
                        role = "user",
                        content = "Revise the response to fully match the data and deliver a complete persuasive report of 150-180 words."
                    )
                )

                val reflectionRequestBody = RequestBody(
                    model = "gpt-4o-mini",
                    messages = messages,
                    maxTokens = 350,
                    temperature = 0f
                )

                val reflectionResponse = nlgEngineRepository.sendChatGPTPrompt(reflectionRequestBody)
                val finalReply = reflectionResponse.choices.firstOrNull()?.message?.content ?: ""

                _response.value = finalReply

                val lastTrip = tripDataRepository.getLastInsertedTrip()
                if (lastTrip == null) {
                    Log.e("ChatGPTViewModel", "No last trip found; skipping report persistence.")
                    return@launch
                }
                val lastTripId = lastTrip.id
                val createdDateTime: LocalDateTime = LocalDateTime.now()
                val zoneId = ZoneId.systemDefault()
                val periodStartDateTime: LocalDateTime
                val periodEndDateTime: LocalDateTime
                if (periodType == PeriodType.LAST_TRIP) {
                    val startDateTime = lastTrip.startDate?.toInstant()
                        ?.atZone(zoneId)
                        ?.toLocalDateTime()
                        ?: Instant.ofEpochMilli(lastTrip.startTime)
                            .atZone(zoneId)
                            .toLocalDateTime()
                    val endDateTime = lastTrip.endDate?.toInstant()
                        ?.atZone(zoneId)
                        ?.toLocalDateTime()
                        ?: lastTrip.endTime?.let {
                            Instant.ofEpochMilli(it).atZone(zoneId).toLocalDateTime()
                        }
                        ?: startDateTime
                    periodStartDateTime = startDateTime
                    periodEndDateTime = endDateTime
                } else {
                    periodStartDateTime = startDate.atStartOfDay()
                    periodEndDateTime = endDate.atStartOfDay()
                }

                val reportEntity = NLGReportEntity(
                    id = UUID.randomUUID(),
                    userId = driverProfileId,
                    startDate = periodStartDateTime,
                    endDate = periodEndDateTime,
                    tripId = lastTripId,
                    reportText = finalReply,
                    createdDate = createdDateTime,
                    sync = false
                )

                runCatching { nlgReportRepository.insertReport(reportEntity) }
                    .onFailure { error ->
                        Log.e("ChatGPTViewModel", "Failed to persist report locally", error)
                    }

                when (val uploadResult = nlgReportApiRepository.createNLGReport(
                    reportEntity.toDomainModel().toNLGReportCreate()
                )) {
                    is Resource.Success -> {
                        nlgReportRepository.updateReport(reportEntity.copy(sync = true))
                    }
                    is Resource.Error -> {
                        Log.e(
                            "ChatGPTViewModel",
                            "Failed to upload report; keeping local copy: ${uploadResult.message}"
                        )
                        notificationManager.displayNotification(
                            "NLG Report Upload",
                            "Upload failed: ${uploadResult.message ?: "Unknown error"}. Will retry."
                        )
                    }
                    Resource.Loading -> {
                        Log.d("ChatGPTViewModel", "Report upload in progress for ${reportEntity.id}")
                    }
                }
            } else {
                _response.value = cachedReport ?: "No cached report available."
            }
        } catch (e: Exception) {
            Log.e("ChatGPTViewModel", "Error fetching chat completion", e)
            _response.value = "Error fetching data"
        } finally {
            _isLoading.value = false
        }
    }
}
}
