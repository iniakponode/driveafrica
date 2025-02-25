package com.uoa.core.nlg.repository

import android.content.Context
import com.uoa.core.network.model.GeminiResponse
import com.uoa.core.network.model.chatGPT.ChatGPTResponse
import com.uoa.core.network.model.chatGPT.OSMResponse
import com.uoa.core.network.model.chatGPT.RequestBody
import com.uoa.core.network.model.nominatim.ReverseGeocodeResponse
import java.util.UUID

interface NLGEngineRepository {
    suspend fun sendChatGPTPrompt(requestBody: RequestBody): ChatGPTResponse
    suspend fun sendGeminiPrompt(context: Context,prompt: String): GeminiResponse
    suspend fun getRoadName(locationID: UUID): ReverseGeocodeResponse
}