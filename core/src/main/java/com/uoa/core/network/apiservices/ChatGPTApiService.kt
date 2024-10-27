package com.uoa.core.network.apiservices

import com.uoa.core.network.model.chatGPT.ChatGPTResponse
import com.uoa.core.network.model.chatGPT.RequestBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ChatGPTApiService {
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun getChatCompletion(@Body requestBody: RequestBody): ChatGPTResponse
}
