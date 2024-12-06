package com.uoa.core.nlg.lngrepositoryimpl.remote.nlgApiRepositoryImpl

import android.content.Context
import android.net.http.HttpException
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresExtension
import com.uoa.core.nlg.repository.NLGEngineRepository
import com.uoa.core.network.apiservices.ChatGPTApiService
import com.uoa.core.network.apiservices.GeminiApiService
import com.uoa.core.network.apiservices.OSMApiService
import com.uoa.core.network.model.GeminiResponse
import com.uoa.core.network.model.chatGPT.ChatGPTResponse
import com.uoa.core.network.model.chatGPT.OSMResponse
import com.uoa.core.network.model.chatGPT.RequestBody
import com.uoa.core.nlg.utils.getGeminiPayload
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

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    override suspend fun sendGeminiPrompt(context: Context, prompt: String): GeminiResponse {
        val payload = getGeminiPayload(prompt)

        try {
            val response = geminiApiService.generateText(payload)
            Log.d("GeminiRepository", "Gemini Response: $response") // Log the response for debugging
            return response
        } catch (e: HttpException) {
            val errorMessage = "HTTP error while sending Gemini prompt: ${e.hashCode()} ${e.message}"
            Log.e("GeminiRepository", errorMessage, e)
            // Handle the error appropriately (e.g., show a user-friendly message, retry, etc.)
            throw e // Re-throw the exception for higher-level handling
        }
    }

    override suspend fun getRoadName(locationID: UUID): OSMResponse {
        // Implement logic to get latitude and longitude from locationID
        val (latitude, longitude) = getLocationCoordinates(locationID)
        return osmApiService.getReverseGeocoding(
            format = "json",
            lat = latitude.toDouble(),
            lon = longitude.toDouble(),
            zoom = 18,
            addressdetails = 1
        )
    }

    // Helper function to retrieve coordinates from locationID
    private suspend fun getLocationCoordinates(locationID: UUID): Pair<Double, Double> {
        // Implement logic to retrieve latitude and longitude from locationID
        // For example, query a local database or a remote API
        // Placeholder implementation:
        val latitude = 0.00 // Replace with actual value
        val longitude = 0.00 // Replace with actual value
        return Pair(latitude, longitude)
    }
}