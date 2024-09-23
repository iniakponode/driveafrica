package com.uoa.core.nlg.lngrepositoryimpl.remote.nlgApiRepositoryImpl

import android.content.Context
import com.uoa.core.nlg.lngrepositoryimpl.NLGEngineRepository
import com.uoa.core.network.apiservices.ChatGPTApiService
import com.uoa.core.network.apiservices.GeminiApiService
import com.uoa.core.network.apiservices.OSMApiService
import com.uoa.core.network.model.Gemini.GeminiRequest
import com.uoa.core.network.model.GeminiResponse
import com.uoa.core.network.model.chatGPT.ChatGPTResponse
import com.uoa.core.network.model.chatGPT.OSMResponse
import com.uoa.core.network.model.chatGPT.RequestBody
import com.uoa.core.nlg.utils.getGeminiPayload
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject


class NLGEngineRepositoryImpl @Inject constructor(
    private val chatGPTApiService: ChatGPTApiService,
    private val geminiApiService: GeminiApiService,
    private val osmApiService: OSMApiService
) : NLGEngineRepository {

    override suspend fun sendChatGPTPrompt(requestBody: RequestBody): ChatGPTResponse {
        return chatGPTApiService.getChatCompletion(requestBody)
    }

    override suspend fun sendGeminiPrompt(context:Context, prompt: String): GeminiResponse {
        val payload = getGeminiPayload(prompt)
        return geminiApiService.generateText(payload)
    }

    override suspend fun getRoadName(locationID: UUID): OSMResponse {
        // Implement logic to get latitude and longitude from locationID
        val (latitude, longitude) = getLocationCoordinates(locationID)
        return osmApiService.getReverseGeocoding(
            format = "json",
            lat = latitude,
            lon = longitude,
            zoom = 18,
            addressdetails = 1
        )
    }

    // Helper function to retrieve coordinates from locationID
    private suspend fun getLocationCoordinates(locationID: UUID): Pair<Long, Long> {
        // Implement logic to retrieve latitude and longitude from locationID
        // For example, query a local database or a remote API
        // Placeholder implementation:
        val latitude = 0L // Replace with actual value
        val longitude = 0L // Replace with actual value
        return Pair(latitude, longitude)
    }
}