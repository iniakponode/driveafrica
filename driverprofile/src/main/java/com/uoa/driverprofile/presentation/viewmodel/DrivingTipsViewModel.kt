package com.uoa.driverprofile.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import com.google.ai.client.generativeai.GenerativeModel
import android.util.Log
import androidx.annotation.RequiresExtension
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.uoa.core.apiServices.services.drivingTipApiService.DrivingTipApiRepository
import com.uoa.core.database.entities.EmbeddingEntity
import com.uoa.core.database.repository.DrivingTipRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.model.DrivingTip
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.network.model.chatGPT.Message
import com.uoa.core.network.model.chatGPT.RequestBody
//import com.uoa.core.nlg.JsonContentBasedRAGEngine
//import com.uoa.core.nlg.repository.EmbeddingUtilsRepository
import com.uoa.core.nlg.repository.NLGEngineRepository
//import com.uoa.core.nlg.RAGEngine
import com.uoa.core.nlg.compressAndEncodeJson
import com.uoa.core.nlg.getRelevantDataFromJson
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.formatDateToUTCPlusOne
import com.uoa.core.utils.localDateToDate
import com.uoa.core.utils.toDrivingTipCreate
import com.uoa.core.utils.toEntity
import com.uoa.core.utils.toTripCreate
import com.uoa.driverprofile.domain.usecase.GetDrivingTipByIdUseCase
import com.uoa.driverprofile.domain.usecase.GetDrivingTipByProfileIdUseCase
import com.uoa.driverprofile.domain.usecase.GetUnsafeBehavioursForTipsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.log

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@HiltViewModel
class DrivingTipsViewModel @Inject constructor(
    private val getDrivingTipByProfileIdUseCase: GetDrivingTipByProfileIdUseCase,
    private val getDrivingTipByIdUseCase: GetDrivingTipByIdUseCase,
    private val drivingTipRepository: DrivingTipRepository,
    private val drivingTipApiRepository: DrivingTipApiRepository,
    private val nlgEngineRepository: NLGEngineRepository,
    private val getUnsafeBehavioursForTipsUseCase: GetUnsafeBehavioursForTipsUseCase,
    private val geminiCaller: GenerativeModel,
    private val tripRepository: TripDataRepository,
    application: Application
) : ViewModel() {

    private val _gptDrivingTips = MutableLiveData<List<DrivingTip>>()
    val gptDrivingTips: LiveData<List<DrivingTip>> get() = _gptDrivingTips

    private val _geminiDrivingTips = MutableLiveData<List<DrivingTip>>()
    val geminiDrivingTips: LiveData<List<DrivingTip>> get() = _geminiDrivingTips

    private val appContext = application.applicationContext

    val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedProfileId = prefs.getString(DRIVER_PROFILE_ID, null)

    init {
        Log.d("DrivingTipsViewModel", "Initializing DrivingTipsViewModel")
        val profileIdString = prefs.getString(DRIVER_PROFILE_ID, null)
        if (profileIdString != null) {
            val profileId = UUID.fromString(profileIdString)
            Log.d("DrivingTipsViewModel", "Profile ID found: $profileId")
            fetchDrivingTips(appContext, profileId)
        } else {
            Log.d("DrivingTipsViewModel", "No profile ID found in shared preferences")
        }
    }

    private fun fetchDrivingTips(context: Context, profileId: UUID) {
        Log.d("DrivingTipsViewModel", "Starting fetchDrivingTips")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val drivingTipsList = getDrivingTipByProfileIdUseCase.execute(profileId)
                val today = LocalDate.now()
                val fourDaysAgo = today.minusDays(4)
                Log.d("DrivingTipsViewModel", "Fetched ${drivingTipsList.size} driving tips")
                if (drivingTipsList.isNotEmpty()) {
                    val tipsNotUsedRecently = drivingTipsList.filter { tip ->
                        tip.date.isBefore(fourDaysAgo) || tip.date.isEqual(fourDaysAgo)
                    }
                    Log.d("DrivingTipsViewModel", "Tips not used recently: ${tipsNotUsedRecently.size}")
                    if (tipsNotUsedRecently.isNotEmpty()) {
                        val selectedTip = tipsNotUsedRecently.random()
                        val updatedTip = selectedTip.copy(date = today)
                        drivingTipRepository.updateDrivingTip(updatedTip.toEntity())
                        Log.d("DrivingTipsViewModel", "Updated driving tip: $updatedTip")
                        withContext(Dispatchers.Main) {
                            if (updatedTip.llm == "gemini") {
                                _geminiDrivingTips.postValue(listOf(updatedTip))
                            } else if (updatedTip.llm == "gpt-3.5-turbo") {
                                _gptDrivingTips.postValue(listOf(updatedTip))
                            }
                            Log.d("DrivingTipsViewModel", "Using existing driving tip")
                        }
                    } else {
                        generateNewDrivingTip(context, profileId)
                    }
                } else {
                    generateNewDrivingTip(context, profileId)
                }
            } catch (ce: CancellationException) {
                Log.w("DrivingTipsViewModel", "fetchDrivingTips cancelled", ce)
                throw ce
            } catch (e: Exception) {
                Log.e("DrivingTipsViewModel", "Error fetching driving tips", e)
            }
        }
    }

    private suspend fun generateNewDrivingTip(context: Context, profileId: UUID) {
        val recentBehaviors = getUnsafeBehavioursForTipsUseCase.execute().toList()
        Log.d("DrivingTipsViewModel", "Recent behaviors: $recentBehaviors")
        if (recentBehaviors.isNotEmpty()) {
            val uniqueBehaviors = recentBehaviors.distinctBy { it.behaviorType }
            val selectedBehavior = uniqueBehaviors.randomOrNull()
            if (selectedBehavior != null) {
                Log.d("DrivingTipsViewModel", "Selected Behavior: ${selectedBehavior.behaviorType}")
                generateAndStoreDrivingTips(context, selectedBehavior, profileId)
            } else {
                Log.d("DrivingTipsViewModel", "No unique behaviors found")
            }
        } else {
            Log.d("DrivingTipsViewModel", "No recent behaviors found")
        }
    }

    private suspend fun generateAndStoreDrivingTips(context: Context, unsafeBehavior: UnsafeBehaviourModel, profileId: UUID) {
        try {
            val tipsPair = generateDrivingTipFromEmbedding(context, unsafeBehavior, profileId)
            if (tipsPair != null) {
                val (geminiTip, gptTip) = tipsPair
                val today = LocalDate.now()
                val newGeminiTip = geminiTip.copy(driverProfileId = profileId, date = today)
                val newGptTip = gptTip.copy(driverProfileId = profileId, date = today)
                drivingTipRepository.insertDrivingTip(newGeminiTip.toEntity())
                drivingTipRepository.insertDrivingTip(newGptTip.toEntity())
                withContext(Dispatchers.IO) {
                    val geminiFormattedDate = formatDateToUTCPlusOne(newGeminiTip.date)
                    val chatGptFormattedDate = formatDateToUTCPlusOne(newGptTip.date)
                    drivingTipApiRepository.createDrivingTip(newGeminiTip.toDrivingTipCreate().copy(date = geminiFormattedDate))
                    drivingTipApiRepository.createDrivingTip(newGptTip.toDrivingTipCreate().copy(date = chatGptFormattedDate))
                    _geminiDrivingTips.postValue(listOf(geminiTip))
                    _gptDrivingTips.postValue(listOf(gptTip))
                    Log.d("DrivingTipsViewModel", "Driving tips generated and posted")
                }
            } else {
                Log.e("DrivingTipsViewModel", "Failed to generate driving tips")
            }
        } catch (ce: CancellationException) {
            Log.w("DrivingTipsViewModel", "generateAndStoreDrivingTips cancelled", ce)
            throw ce
        } catch (e: Exception) {
            Log.e("DrivingTipsViewModel", "Error generating and storing driving tips", e)
        }
    }

    private suspend fun generateDrivingTipFromEmbedding(
        context: Context,
        unsafeBehavior: UnsafeBehaviourModel,
        profileId: UUID
    ): Pair<DrivingTip, DrivingTip>? {
        Log.d("DrivingTipsViewModel", "Starting generateDrivingTipFromEmbedding")
        return withContext(Dispatchers.IO) {
            try {
                val contextText = compressAndEncodeJson(context)
                val prompt = createAIPromptFromChunks(unsafeBehavior, contextText, context)
                Log.d("DrivingTipsViewModel", "Generated prompt for AI models")
                val geminiTipContent = runCatching {
                    geminiCaller.generateContent(prompt)
                }.getOrNull()?.text?.let { parseTipContent(it, "gemini", profileId) }
                val requestBody = RequestBody(
                    model = "gpt-3.5-turbo",
                    messages = listOf(Message(role = "user", content = prompt)),
                    maxTokens = 750,
                    temperature = 0.5f
                )
                val chatGPTResponse = nlgEngineRepository.sendChatGPTPrompt(requestBody)
                val chatGPTTipContent = chatGPTResponse.choices.firstOrNull()?.message?.content?.let {
                    parseTipContent(it, "gpt-3.5-turbo", profileId)
                }
                if (geminiTipContent != null && chatGPTTipContent != null) {
                    Log.d("DrivingTipsViewModel", "Successfully generated driving tips")
                    Log.d("DrivingTipsViewModel", "Gemini Response: $geminiTipContent \nGPT Response: $chatGPTTipContent")
                    Pair(geminiTipContent, chatGPTTipContent)
                } else {
                    Log.e("DrivingTipsViewModel", "Failed to generate driving tips from AI responses")
                    null
                }
            } catch (ce: CancellationException) {
                Log.w("DrivingTipsViewModel", "Generation cancelled", ce)
                throw ce
            } catch (e: Exception) {
                Log.e("DrivingTipsViewModel", "Error generating driving tips from embedding", e)
                null
            }
        }
    }

    private suspend fun createAIPromptFromChunks(
        unsafeBehavior: UnsafeBehaviourModel,
        contextText: String,
        context: Context
    ): String {
        var unsafeBehaviorType = ""
        if (unsafeBehavior.behaviorType == "Speeding") {
            unsafeBehaviorType = "Speed Limit Violation"
            Log.d("createAIPrompt", "Unsafe behavior type is $unsafeBehaviorType")
        } else if (unsafeBehavior.behaviorType == "Swerving" ||
            unsafeBehavior.behaviorType == "Harsh Braking" ||
            unsafeBehavior.behaviorType == "Harsh Acceleration"
        ) {
            unsafeBehaviorType = "Dangerous or Reckless Driving"
            Log.d("createAIPrompt", "Unsafe behavior type is $unsafeBehaviorType")
        }
        val relevantJsonData = getRelevantDataFromJson(context, unsafeBehaviorType)
        val behaviorType = unsafeBehavior.behaviorType
        val trip = tripRepository.getTripById(unsafeBehavior.tripId)
        val cause = trip?.influence
        return if (relevantJsonData != null) {
            """
            You are an expert in Nigerian driving laws. Using the provided context and relevant regulations, generate a driving tip in strict JSON format. You must include fines and law sections exactly as provided in the context. No new information or hallucinated values should be introduced in the response.
            
            Unsafe Behavior: $behaviorType
            Cause: $cause
            Context (Base64-encoded JSON): $contextText
            
            The relevant provided regulations, fines, and penalties are:
            
            $relevantJsonData
            
            Ensure the following in your response:
            1. The **fine** and **law** values must come directly from the context provided.
            2. Do not generate or hallucinate any fines, laws, or penalties beyond the provided context.
            
            Please format the output as valid JSON:
            
            {
                "title": "Your Tip Title",
                "meaning": "Supportive explanation of the behavior.",
                "penalty": "Applicable penalties based strictly on the provided context.",
                "fine": "Applicable fines strictly based on the provided context.",
                "law": "Law sections strictly from the provided context.",
                "summaryTip": "Encouraging, actionable advice based on the context."
            }
            """
        } else {
            """
            You are an expert in Nigerian driving laws. Using the provided context, generate a driving tip in strict JSON format. You must include fines and law sections exactly as provided in the context. No new information or hallucinated values should be introduced in the response.
            
            Unsafe Behavior: $behaviorType
            Cause: $cause
            Context (Base64-encoded JSON): $contextText
            
            No relevant fines or laws were found for this behavior in the context provided.
            
            Ensure the following in your response:
            1. The **fine** and **law** values must come directly from the context provided.
            2. Do not generate or hallucinate any fines, laws, or penalties beyond the provided context.
            
            Please format the output as valid JSON:
            
            {
                "title": "Your Tip Title",
                "meaning": "Supportive explanation of the behavior.",
                "penalty": "Applicable penalties based strictly on the provided context.",
                "fine": "Applicable fines strictly based on the provided context.",
                "law": "Law sections strictly from the provided context.",
                "summaryTip": "Encouraging, actionable advice based on the context."
            }
            """
        }.trimIndent()
    }

    private fun parseTipContent(aiResponse: String, llm: String, profileId: UUID): DrivingTip? {
        Log.d("DrivingTipsViewModel", "Parsing AI response")
        val jsonString = extractJsonObject(aiResponse)
        if (jsonString == null) {
            Log.e("DrivingTipsViewModel", "No JSON object found in the AI response")
            return null
        }
        return try {
            val jsonObject = JSONObject(jsonString)
            DrivingTip(
                tipId = UUID.randomUUID(),
                title = jsonObject.getString("title"),
                meaning = jsonObject.getString("meaning"),
                penalty = jsonObject.optString("penalty", null),
                fine = jsonObject.optString("fine", null),
                law = jsonObject.optString("law", null),
                hostility = "",
                summaryTip = jsonObject.getString("summaryTip"),
                date = LocalDate.now(),
                driverProfileId = profileId,
                llm = llm
            )
        } catch (e: JSONException) {
            Log.e("DrivingTipsViewModel", "Error parsing AI response", e)
            val fixedJsonString = attemptToFixJson(jsonString)
            if (fixedJsonString != null) {
                try {
                    val jsonObject = JSONObject(fixedJsonString)
                    DrivingTip(
                        tipId = UUID.randomUUID(),
                        title = jsonObject.getString("title"),
                        meaning = jsonObject.getString("meaning"),
                        penalty = jsonObject.optString("penalty", null),
                        fine = jsonObject.optString("fine", null),
                        law = jsonObject.optString("law", null),
                        hostility = "",
                        summaryTip = jsonObject.getString("summaryTip"),
                        date = LocalDate.now(),
                        driverProfileId = profileId,
                        llm = llm
                    )
                } catch (ex: JSONException) {
                    Log.e("DrivingTipsViewModel", "Error parsing fixed AI response", ex)
                    null
                }
            } else {
                null
            }
        }
    }

    private fun extractJsonObject(response: String): String? {
        val startIndex = response.indexOf('{')
        if (startIndex == -1) return null
        var braceCount = 0
        var endIndex = -1
        for (i in startIndex until response.length) {
            when (response[i]) {
                '{' -> braceCount++
                '}' -> {
                    braceCount--
                    if (braceCount == 0) {
                        endIndex = i
                        break
                    }
                }
            }
        }
        return if (endIndex != -1) response.substring(startIndex, endIndex + 1) else null
    }

    private fun attemptToFixJson(jsonString: String): String? {
        var fixedJson = jsonString.trim()
        val unclosedStringPattern = Regex("""(".*?)(?:\n|$)""", RegexOption.DOT_MATCHES_ALL)
        fixedJson = unclosedStringPattern.replace(fixedJson) { matchResult ->
            val value = matchResult.value
            val quotesCount = value.count { it == '"' }
            if (quotesCount % 2 != 0) "$value\"" else value
        }
        if (!fixedJson.endsWith("}")) fixedJson += "}"
        if (!fixedJson.startsWith("{")) fixedJson = "{$fixedJson"
        return try {
            JSONObject(fixedJson)
            fixedJson
        } catch (e: JSONException) {
            Log.e("DrivingTipsViewModel", "Fixed JSON is still invalid", e)
            null
        }
    }

    suspend fun getDrivingTipById(drivingTipId: UUID): DrivingTip {
        Log.d("DrivingTipsViewModel", "Getting driving tip by ID: $drivingTipId")
        return withContext(Dispatchers.IO) {
            getDrivingTipByIdUseCase.execute(drivingTipId)
        }
    }

    private fun formatDateToUTCPlusOne(date: LocalDate): String {
        // Implement your date formatting logic here
        return date.toString()
    }
}