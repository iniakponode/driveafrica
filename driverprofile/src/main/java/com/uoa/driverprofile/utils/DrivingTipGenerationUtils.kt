package com.uoa.driverprofile.utils

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.model.DrivingTip
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.network.model.chatGPT.Message
import com.uoa.core.network.model.chatGPT.RequestBody
import com.uoa.core.nlg.compressAndEncodeJson
import com.uoa.core.nlg.getRelevantDataFromJson
import com.uoa.core.nlg.repository.NLGEngineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.StringReader
import java.util.UUID

suspend fun generateDrivingTipPair(
    context: Context,
    unsafeBehavior: UnsafeBehaviourModel,
    profileId: UUID,
    geminiCaller: GenerativeModel,
    nlgEngineRepository: NLGEngineRepository,
    tripRepository: TripDataRepository,
    generateGemini: Boolean,
    generateGpt: Boolean
): Pair<DrivingTip?, DrivingTip?>? = withContext(Dispatchers.IO) {
    try {
        val contextText = compressAndEncodeJson(context)
        val prompt = createTipPrompt(unsafeBehavior, contextText, context, tripRepository)

        val geminiTipContent = if (generateGemini) {
            runCatching {
                geminiCaller.generateContent(prompt)
            }.getOrNull()?.text?.let { parseTipContent(it, "gemini", profileId) }
        } else {
            null
        }

        val chatGPTTipContent = if (generateGpt) {
            val requestBody = RequestBody(
                model = "gpt-4-turbo",
                messages = listOf(Message(role = "user", content = prompt)),
                maxTokens = 750,
                temperature = 0.5f
            )
            val chatGPTResponse = nlgEngineRepository.sendChatGPTPrompt(requestBody)
            chatGPTResponse.choices.firstOrNull()?.message?.content?.let {
                parseTipContent(it, "gpt-4-turbo", profileId)
            }
        } else {
            null
        }

        if (geminiTipContent == null && chatGPTTipContent == null) {
            Log.e("DrivingTipGenerator", "Failed to generate tips from AI responses")
            return@withContext null
        }
        Log.d("DrivingTipGenerator", "Generated tips successfully")
        Pair(geminiTipContent, chatGPTTipContent)
    } catch (e: Exception) {
        Log.e("DrivingTipGenerator", "Error generating tips", e)
        null
    }
}

private suspend fun createTipPrompt(
    unsafeBehavior: UnsafeBehaviourModel,
    contextText: String,
    context: Context,
    tripRepository: TripDataRepository
): String {
    val unsafeBehaviorType = when (unsafeBehavior.behaviorType) {
        "Speeding", "Rough Road Speeding" -> "Speed Limit Violation"
        "Swerving",
        "Harsh Braking",
        "Harsh Acceleration",
        "Aggressive Turn",
        "Aggressive Stop-and-Go" -> "Dangerous or Reckless Driving"
        "Phone Handling" -> "Use your GSM phone while driving"
        "Fatigue" -> "be asleep while driving"
        "Crash Detected" -> "Reporting of road crashes"
        else -> unsafeBehavior.behaviorType
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
        """.trimIndent()
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
        """.trimIndent()
    }
}

private fun parseTipContent(aiResponse: String, llm: String, profileId: UUID): DrivingTip? {
    val jsonString = extractJsonObject(aiResponse)
    if (jsonString == null) {
        Log.e(
            "DrivingTipGenerator",
            "AI response missing JSON. Raw: ${responsePreview(aiResponse)}"
        )
        return null
    }

    val jsonObject = parseJsonObjectLenient(jsonString)
        ?: attemptToFixJson(jsonString)?.let { parseJsonObjectLenient(it) }
    if (jsonObject == null) {
        Log.e(
            "DrivingTipGenerator",
            "Error parsing AI response. Raw: ${responsePreview(aiResponse)}"
        )
        return null
    }

    val title = jsonObject.stringOrNull("title")
        ?: jsonObject.stringOrNull("tipTitle")
        ?: "Driving Tip"
    val meaning = jsonObject.stringOrNull("meaning")
        ?: jsonObject.stringOrNull("explanation")
        ?: jsonObject.stringOrNull("description")
    val summaryTip = jsonObject.stringOrNull("summaryTip")
        ?: jsonObject.stringOrNull("summary")
        ?: meaning
    val penalty = jsonObject.stringOrNull("penalty")
    val fine = jsonObject.stringOrNull("fine")
    val law = jsonObject.stringOrNull("law")

    if (title == "Driving Tip" && meaning.isNullOrBlank() && summaryTip.isNullOrBlank()) {
        Log.e(
            "DrivingTipGenerator",
            "AI response missing required fields. Raw: ${responsePreview(aiResponse)}"
        )
        return null
    }

    return DrivingTip(
        tipId = UUID.randomUUID(),
        title = title,
        meaning = meaning,
        penalty = penalty,
        fine = fine,
        law = law,
        hostility = "",
        summaryTip = summaryTip,
        date = java.time.LocalDate.now(),
        driverProfileId = profileId,
        llm = llm
    )
}

private fun extractJsonObject(response: String): String? {
    val cleaned = stripCodeFences(response).trim()
    val objectIndex = cleaned.indexOf('{')
    val arrayIndex = cleaned.indexOf('[')
    val startIndex = when {
        objectIndex == -1 && arrayIndex == -1 -> -1
        objectIndex == -1 -> arrayIndex
        arrayIndex == -1 -> objectIndex
        else -> minOf(objectIndex, arrayIndex)
    }
    if (startIndex == -1) return null
    val openChar = cleaned[startIndex]
    val closeChar = if (openChar == '[') ']' else '}'
    var depth = 0
    var endIndex = -1
    for (i in startIndex until cleaned.length) {
        when (cleaned[i]) {
            openChar -> depth++
            closeChar -> {
                depth--
                if (depth == 0) {
                    endIndex = i
                    break
                }
            }
        }
    }
    return if (endIndex != -1) cleaned.substring(startIndex, endIndex + 1) else null
}

private fun attemptToFixJson(jsonString: String): String? {
    var fixedJson = stripCodeFences(jsonString).trim()
    val unclosedStringPattern = Regex("""(".*?)(?:\n|$)""", RegexOption.DOT_MATCHES_ALL)
    fixedJson = unclosedStringPattern.replace(fixedJson) { matchResult ->
        val value = matchResult.value
        val quotesCount = value.count { it == '"' }
        if (quotesCount % 2 != 0) "$value\"" else value
    }
    fixedJson = fixedJson.replace(Regex(",\\s*([}\\]])"), "$1")
    if (!fixedJson.endsWith("}")) fixedJson += "}"
    if (!fixedJson.startsWith("{")) fixedJson = "{$fixedJson"
    return try {
        JSONObject(fixedJson)
        fixedJson
    } catch (e: JSONException) {
        Log.e("DrivingTipGenerator", "Fixed JSON is still invalid", e)
        null
    }
}

private fun parseJsonObjectLenient(jsonString: String): JsonObject? {
    return runCatching {
        val reader = JsonReader(StringReader(jsonString)).apply { isLenient = true }
        val element = JsonParser.parseReader(reader)
        when {
            element.isJsonObject -> element.asJsonObject
            element.isJsonArray -> {
                val array = element.asJsonArray
                if (array.size() == 0 || !array[0].isJsonObject) null else array[0].asJsonObject
            }
            else -> null
        }
    }.getOrNull()
}

private fun JsonObject.stringOrNull(name: String): String? {
    val value = get(name) ?: return null
    if (value.isJsonNull) return null
    val text = runCatching { value.asString }.getOrNull() ?: return null
    val trimmed = text.trim()
    return trimmed.takeIf { it.isNotBlank() }
}

private fun stripCodeFences(response: String): String {
    val fenced = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```").find(response)
    return fenced?.groupValues?.get(1) ?: response
}

private fun responsePreview(response: String): String {
    val compact = response.replace(Regex("\\s+"), " ").trim()
    return if (compact.length <= 200) compact else compact.substring(0, 200) + "..."
}
