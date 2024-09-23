package com.uoa.core.nlg.utils

import android.content.Context
import com.uoa.core.network.model.Gemini.GeminiRequest
import com.uoa.core.network.model.Gemini.Prompt

fun getApiKeyFromSecureStorage(context: Context): String {
    // Implement secure retrieval of ChatGPT API key
    // Example: Use EncryptedSharedPreferences or Android Keystore
    return "YOUR_CHATGPT_API_KEY" // Replace with actual implementation
}

fun getGeminiApiKey(context: Context): String {
    // Implement secure retrieval of Gemini API key
    return "YOUR_GEMINI_API_KEY" // Replace with actual implementation
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