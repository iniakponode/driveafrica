package com.uoa.core.network.apiservices

import com.uoa.core.network.model.Gemini.GeminiRequest
import com.uoa.core.network.model.GeminiResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface GeminiApiService {
        @POST("v1beta2/models/text-bison-001:generateText")
        suspend fun generateText(@Body payload: GeminiRequest): GeminiResponse
}