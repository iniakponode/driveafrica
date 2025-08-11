package com.uoa.core.nlg.utils

import android.content.Context
import com.uoa.core.BuildConfig
import com.uoa.core.network.model.Gemini.GeminiRequest
import com.uoa.core.network.model.Gemini.Prompt

/**
 * Retrieve the ChatGPT API key from configuration.
 *
 * Keys are supplied via `local.properties` or environment variables and
 * exposed to the app through BuildConfig fields to avoid hard‑coding.
 */
fun getApiKeyFromSecureStorage(context: Context): String {
    return BuildConfig.CHAT_GPT_API_KEY
}

/**
 * Retrieve the Gemini API key from configuration.
 *
 * Keys are supplied via `local.properties` or environment variables and
 * exposed to the app through BuildConfig fields.
 */
fun getGeminiApiKey(context: Context): String {
    return BuildConfig.GEMINI_API_KEY
}

fun getGeminiPayload(promptText: String): GeminiRequest {
    // Securely retrieve your Gemini API key if needed
    // Construct the request object
    return GeminiRequest(
        model = "models/text-bison-001", // Replace with your model
        prompt = Prompt(text = promptText),
        temperature = 0.5,
        candidateCount = 1,
        topK = 40,
        topP = 0.95
    )
}