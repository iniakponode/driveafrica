package com.uoa.core.network.model.chatGPT

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class ChatGPTResponse(
    @field:SerializedName("id")
    val id: String,
    @field:SerializedName("object")
    val `object`: String,
    @field:SerializedName("created")
    val created: Long,
    @field:SerializedName("choices")
    val choices: List<Choice>,
    @field:SerializedName("usage")
    val usage: Usage
)

@Keep
data class Choice(
    @field:SerializedName("index")
    val index: Int,
    @field:SerializedName("message")
    val message: Message,
    @field:SerializedName("finish_reason")
    val finishReason: String
)

@Keep
data class Usage(
    @field:SerializedName("prompt_tokens")
    val promptTokens: Int,
    @field:SerializedName("completion_tokens")
    val completionTokens: Int,
    @field:SerializedName("total_tokens")
    val totalTokens: Int
)
