package com.uoa.core.nlg.lngrepositoryimpl.remote.nlgApiRepositoryImpl

import android.content.Context
import android.net.http.HttpException
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresExtension
import com.uoa.core.nlg.repository.NLGEngineRepository
import com.uoa.core.network.apiservices.ChatGPTApiService
import com.uoa.core.network.apiservices.GeminiApiService
import com.uoa.core.network.apiservices.OSMRoadApiService
import com.uoa.core.network.model.GeminiResponse
import com.uoa.core.network.model.chatGPT.ChatGPTResponse
import com.uoa.core.network.model.chatGPT.RequestBody
import com.uoa.core.network.model.nominatim.ReverseGeocodeResponse
import com.uoa.core.nlg.utils.getGeminiPayload
import kotlinx.coroutines.delay
import java.util.UUID
import javax.inject.Inject


class NLGEngineRepositoryImpl @Inject constructor(
    private val chatGPTApiService: ChatGPTApiService,
    private val geminiApiService: GeminiApiService,
    private val osmRoadApiService: OSMRoadApiService
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

    // Single call that retrieves the road name from OSM given a locationID
    override suspend fun getRoadName(locationID: UUID): ReverseGeocodeResponse {
        // Implement logic to get latitude and longitude from locationID
        val (latitude, longitude) = getLocationCoordinates(locationID)

        // Comply with usage policy: at most 1 request/sec
        // Optional: If this is the only place calling OSM, you could do a delay(1000)
        // or a more robust rate-limiter approach
        delay(1000) // naive approach: wait 1 second before each call
        return osmRoadApiService.reverseGeocode(
            lat = latitude.toDouble(),
            lon = longitude.toDouble(),
            // Use "jsonv2" to get a fuller structure
            format = "jsonv2"
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