package com.uoa.driverprofile.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import com.google.ai.client.generativeai.GenerativeModel
import android.util.Log
import androidx.annotation.RequiresExtension
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
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
import com.uoa.core.utils.toEntity
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
import kotlin.math.log

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@HiltViewModel
class DrivingTipsViewModel @Inject constructor(
    private val getDrivingTipByProfileIdUseCase: GetDrivingTipByProfileIdUseCase,
    private val getDrivingTipByIdUseCase: GetDrivingTipByIdUseCase,
    private val drivingTipRepository: DrivingTipRepository,
    private val nlgEngineRepository: NLGEngineRepository,
    private val getUnsafeBehavioursForTipsUseCase: GetUnsafeBehavioursForTipsUseCase,
    private val geminiCaller: GenerativeModel,
//    private val embeddingUtilsRepository: EmbeddingUtilsRepository,
//    private val ragEngine: JsonContentBasedRAGEngine,
    private val tripRepository: TripDataRepository,
    application: Application
) : ViewModel() {

    private val _gptDrivingTips = MutableLiveData<List<DrivingTip>>()
    val gptDrivingTips: LiveData<List<DrivingTip>> get() = _gptDrivingTips

    private val _geminiDrivingTips = MutableLiveData<List<DrivingTip>>()
    val geminiDrivingTips: LiveData<List<DrivingTip>> get() = _geminiDrivingTips

    private val appContext = application.applicationContext

    init {
        Log.d("DrivingTipsViewModel", "Initializing DrivingTipsViewModel")
        // Fetch profile ID and start fetching driving tips
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val profileIdString = prefs.getString(DRIVER_PROFILE_ID, null)
        if (profileIdString != null) {
            val profileId = UUID.fromString(profileIdString)
            Log.d("DrivingTipsViewModel", "Profile ID found: $profileId")
            // Initialize RAGEngine
//            ragEngine.initialize(application.applicationContext)
//            generateAndStoreEmbeddingsIfNone(appContext)
            fetchDrivingTips(appContext,profileId)
        } else {
            Log.d("DrivingTipsViewModel", "No profile ID found in shared preferences")
        }
    }

//    private fun generateAndStoreEmbeddingsIfNone(context: Context) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val embeddings = embeddingUtilsRepository.getAllEmbeddings()
//            if (embeddings.isEmpty()) {
//                // Generate and store new embeddings
//                ragEngine.embedOffencesJson(context)
//                 }
//            }
//        }

    private fun fetchDrivingTips(context: Context,profileId: UUID) {
        Log.d("DrivingTipsViewModel", "Starting fetchDrivingTips")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch existing driving tips for the profileId
                val drivingTipsList = getDrivingTipByProfileIdUseCase.execute(profileId)


                val today = LocalDate.now()
                val fourDaysAgo = today.minusDays(4)
                Log.d("DrivingTipsViewModel", "Fetched ${drivingTipsList.size} driving tips")
                if (drivingTipsList.isNotEmpty()) {
                    // Filter tips not used in the last 4 days (including today)
                    val tipsNotUsedRecently = drivingTipsList.filter { tip ->
                        tip.date.isBefore(fourDaysAgo) || tip.date.isEqual(fourDaysAgo)
                    }
                    Log.d("DrivingTipsViewModel", "Tips not used recently: ${tipsNotUsedRecently.size}")

                    if (tipsNotUsedRecently.isNotEmpty()) {
                        // Pick one tip that has not been used recently
                        val selectedTip = tipsNotUsedRecently.random()

                        // Update the tip's date to today to mark it as used
                        val updatedTip = selectedTip.copy(date = today)
                        drivingTipRepository.updateDrivingTip(updatedTip.toEntity())
                        Log.d("DrivingTipsViewModel", "Updated driving tip: $updatedTip")
                        // Post the tip to LiveData on the main thread
                        withContext(Dispatchers.Main) {
                            if (updatedTip.llm == "gemini") {
                                _geminiDrivingTips.value = listOf(updatedTip)
                            } else if (updatedTip.llm == "gpt-3.5-turbo") {
                                _gptDrivingTips.value = listOf(updatedTip)
                            }
                            Log.d("DrivingTipsViewModel", "Using existing driving tip")
                        }
                    } else {
                        // All tips have been used recently, generate a new tip
                        generateNewDrivingTip(context,profileId)
                    }
                } else {
                    // No existing tips, generate a new tip
                    generateNewDrivingTip(context,profileId)
                }
            } catch (e: Exception) {
                Log.e("DrivingTipsViewModel", "Error fetching driving tips", e)
            }
        }
    }

    private suspend fun generateAndStoreDrivingTips(context: Context, unsafeBehavior: UnsafeBehaviourModel, profileId: UUID) {
        try {
            // Generate driving tips from embeddings
            val tipsPair = generateDrivingTipFromEmbedding(context,unsafeBehavior,profileId)
            if (tipsPair != null) {
                val (geminiTip, gptTip) = tipsPair

                // Assign the correct profile ID and set the date to today
                val today = LocalDate.now()
                val newGeminiTip = geminiTip.copy(profileId = profileId, date = today)
                val newGptTip = gptTip.copy(profileId = profileId, date = today)

                // Insert tips into the repository
                drivingTipRepository.insertDrivingTip(newGeminiTip.toEntity())
                drivingTipRepository.insertDrivingTip(newGptTip.toEntity())

                // Post the tips to LiveData on the main thread
                withContext(Dispatchers.Main) {
                    _geminiDrivingTips.value = listOf(geminiTip)
                    _gptDrivingTips.value = listOf(gptTip)
                    Log.d("DrivingTipsViewModel", "Driving tips generated and posted")
                }
            } else {
                Log.e("DrivingTipsViewModel", "Failed to generate driving tips")
            }
        } catch (e: Exception) {
            Log.e("DrivingTipsViewModel", "Error generating and storing driving tips", e)
        }
    }

    private suspend fun generateNewDrivingTip(context: Context, profileId: UUID) {
        // Fetch recent unsafe behaviors
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



    private suspend fun generateDrivingTipFromEmbedding(context: Context, unsafeBehavior: UnsafeBehaviourModel, profileId: UUID): Pair<DrivingTip, DrivingTip>? {
        Log.d("DrivingTipsViewModel", "Starting generateDrivingTipFromEmbedding")
        return withContext(Dispatchers.IO) {
            try {
                // Generate embedding from the unsafe behavior description now old implementation code
//                var unsafeBehaviorType=""
//                if (unsafeBehavior.behaviorType == "Speeding") {
//                    unsafeBehaviorType = "Speed Limit Violation"
//                    Log.e("DrivingTipsViewModel", "Unsafe behavior type is $unsafeBehaviorType")
//
//                }
//                else if (unsafeBehavior.behaviorType == "Swerving"
//                    ||unsafeBehavior.behaviorType =="Harsh Braking"
//                    ||unsafeBehavior.behaviorType=="Harsh Acceleration") {
//                    unsafeBehaviorType = "Dangerous or Reckless Driving"
//                    Log.e("DrivingTipsViewModel", "Unsafe behavior type is $unsafeBehaviorType")
//                }
//
//                val embeddings = ragEngine.generateEmbeddingFromChunk(unsafeBehaviorType)
//                if (embeddings.isEmpty()) {
//                    Log.e("DrivingTipsViewModel", "Failed to generate embeddings")
//                    return@withContext null
//                }
//
//                // Find similar chunks from memory
//                val similarChunks = ragEngine.findSimilarChunksInMemory(embeddings)
//                Log.d("DrivingTipsViewModel", "Found ${similarChunks.size} similar chunks")
//                similarChunks.forEach{
//                    Log.d("DrivingTipsViewModel", "Found Similar Chunk ${it.first} ${it.second} ${it.third}")
//                }

                // Create prompt for AI models
//                New implementation code with Json Knowledge Base File.
//                This made us to include the context parameter in the function
                val contextText=compressAndEncodeJson(context)
                val prompt = createAIPromptFromChunks(unsafeBehavior, contextText, context)

//                old implementation code with embeddings
//                val prompt = createAIPromptFromChunks(similarChunks, unsafeBehavior)
                Log.d("DrivingTipsViewModel", "Generated prompt for AI models")

                // Gemini API call
                val geminiResponse = geminiCaller.generateContent(prompt)
                val geminiTipContent = geminiResponse.text?.let { parseTipContent(it, "gemini", profileId) }

                // ChatGPT API call
                val requestBody = RequestBody(
                    model = "gpt-3.5-turbo",
                    messages = listOf(Message(role = "user", content = prompt)),
                    maxTokens = 750, // Increased from 200
                    temperature = 0.5f
                )
                val chatGPTResponse = nlgEngineRepository.sendChatGPTPrompt(requestBody)
                val chatGPTTipContent = chatGPTResponse.choices.firstOrNull()?.message?.content?.let {
                    parseTipContent(it, "gpt-3.5-turbo", profileId)
                }

                if (geminiTipContent != null && chatGPTTipContent != null) {
                    Log.d("DrivingTipsViewModel", "Successfully generated driving tips")
                    Log.d("DrivingViewModel", "Gemini Response: $geminiTipContent \nGPT Response: $chatGPTTipContent")

//                    drivingTipRepository.insertDrivingTip(geminiTipContent.toEntity())
//                    drivingTipRepository.insertDrivingTip(chatGPTTipContent.toEntity())
                    Pair(geminiTipContent, chatGPTTipContent)
                } else {
                    Log.e("DrivingTipsViewModel", "Failed to generate driving tips from AI responses")
                    null
                }
            } catch (e: Exception) {
                Log.e("DrivingTipsViewModel", "Error generating driving tips from embedding", e)
                null
            }
        }
    }


//    Now Old for embeddings implementation
//    private fun createAIPromptFromChunks(
////        similarChunks: List<Triple<String, String, Float>>,
//        unsafeBehavior: UnsafeBehaviourModel,
//        contextText: String
//    ): String {
////        val contextText = similarChunks.joinToString(separator = "\n") {
////            val source = when (it.second) {
////                "nat_dr_reg_law" -> "Nigerian Driving Regulations"
////                "ng_high_way_code" -> "Nigerian Highway Code"
////                else -> "Unknown Source"
////            }
////            "Source: $source\nText: ${it.first}"
////        }
//
//
//        val behaviorType = unsafeBehavior.behaviorType
//        val cause = if (unsafeBehavior.alcoholInfluence) "Alcohol" else "No Alcohol"
//        return """
//                You are an expert in Nigerian driving laws. Generate a helpful driving tip using the following information:
//
//                Unsafe Behavior: $behaviorType
//
//                Cause: $cause
//
//                Context: $contextText
//
//                Instructions:
//
//                - Provide the output strictly in JSON format as specified.
//                - Do not include any additional text or explanations.
//                - Ensure the JSON is properly formatted and valid.
//
//                **JSON Format:**
//                {
//                    "title": "Your Tip Title",
//                    "meaning": "Supportive explanation of the behavior.",
//                    "penalty": "Applicable penalties in a neutral tone.",
//                    "fine": "Applicable fines in a neutral tone.",
//                    "law": "Relevant laws stated factually.",
//                    "summaryTip": "Encouraging, actionable advice."
//                }
//    """.trimIndent()
//
//    }

    private suspend fun createAIPromptFromChunks(
        unsafeBehavior: UnsafeBehaviourModel,
        contextText: String,
        context: Context
    ): String {
        // Determine the behavior type to match in the JSON
        var unsafeBehaviorType = ""
        if (unsafeBehavior.behaviorType == "Speeding") {
            unsafeBehaviorType = "Speed Limit Violation"
            Log.d("createAIPrompt", "Unsafe behavior type is $unsafeBehaviorType")
        } else if (unsafeBehavior.behaviorType == "Swerving"
            || unsafeBehavior.behaviorType == "Harsh Braking"
            || unsafeBehavior.behaviorType == "Harsh Acceleration") {
            unsafeBehaviorType = "Dangerous or Reckless Driving"
            Log.d("createAIPrompt", "Unsafe behavior type is $unsafeBehaviorType")
        }

        // Get relevant JSON data based on the unsafe behavior type
        val relevantJsonData = getRelevantDataFromJson(context, unsafeBehaviorType)

        // Prepare a base prompt text
        val behaviorType = unsafeBehavior.behaviorType

        val trip=tripRepository.getTripById(unsafeBehavior.tripId)

//                Count number of alcohol influenced trips


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
        // Extract the JSON object from the response
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
                profileId = profileId, // Will be updated later
                llm = llm
            )
        } catch (e: JSONException) {
            Log.e("DrivingTipsViewModel", "Error parsing AI response", e)
            // Optionally, attempt to fix common JSON errors
            val fixedJsonString = attemptToFixJson(jsonString)
            return if (fixedJsonString != null) {
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
                        profileId = profileId, // Will be updated later
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
        if (startIndex == -1) {
            return null
        }

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

        return if (endIndex != -1) {
            response.substring(startIndex, endIndex + 1)
        } else {
            null
        }
    }


    private fun attemptToFixJson(jsonString: String): String? {
        var fixedJson = jsonString.trim()

        // Attempt to fix unclosed strings by adding missing quotation marks
        val unclosedStringPattern = Regex("""(".*?)(?:\n|$)""", RegexOption.DOT_MATCHES_ALL)
        fixedJson = unclosedStringPattern.replace(fixedJson) { matchResult ->
            val value = matchResult.value
            val quotesCount = value.count { it == '"' }
            if (quotesCount % 2 != 0) {
                // Add a closing quotation mark
                "$value\""
            } else {
                value
            }
        }

        // Ensure JSON ends with a closing brace
        if (!fixedJson.endsWith("}")) {
            fixedJson += "}"
        }

        // Ensure JSON starts with an opening brace
        if (!fixedJson.startsWith("{")) {
            fixedJson = "{$fixedJson"
        }

        // Try parsing the fixed JSON to verify it's valid
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
}