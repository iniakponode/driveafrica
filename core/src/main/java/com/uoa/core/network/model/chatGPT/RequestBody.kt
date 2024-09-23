package com.uoa.core.network.model.chatGPT

import com.google.gson.annotations.SerializedName

data class RequestBody(
    @SerializedName("model")
    val model: String,

    @SerializedName("messages")
    val messages: List<Message>,

    @SerializedName("max_tokens")
    val maxTokens: Int? = null,

    @SerializedName("temperature")
    val temperature: Float? = null,

    @SerializedName("top_p")
    val topP: Float? = null,

    @SerializedName("frequency_penalty")
    val frequencyPenalty: Float? = null,

    @SerializedName("presence_penalty")
    val presencePenalty: Float? = null,

    @SerializedName("stop")
    val stop: String? = null
)

data class Message(
    @SerializedName("role")
    val role: String,

    @SerializedName("content")
    val content: String
)

