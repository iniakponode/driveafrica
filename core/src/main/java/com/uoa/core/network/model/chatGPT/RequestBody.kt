package com.uoa.core.network.model.chatGPT

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class RequestBody(
    @field:SerializedName("model")
    val model: String,

    @field:SerializedName("messages")
    val messages: List<Message>,

    @field:SerializedName("max_tokens")
    val maxTokens: Int? = null,

    @field:SerializedName("temperature")
    val temperature: Float? = null,

    @field:SerializedName("top_p")
    val topP: Float? = null,

    @field:SerializedName("frequency_penalty")
    val frequencyPenalty: Float? = null,

    @field:SerializedName("presence_penalty")
    val presencePenalty: Float? = null,

    @field:SerializedName("stop")
    val stop: String? = null
)

@Keep
data class Message(
    @field:SerializedName("role")
    val role: String,

    @field:SerializedName("content")
    val content: String
)

