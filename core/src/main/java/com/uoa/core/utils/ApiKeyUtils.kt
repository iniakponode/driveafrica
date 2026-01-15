package com.uoa.core.utils

import android.util.Log
import com.uoa.core.BuildConfig

object ApiKeyUtils {
    private const val TAG = "ApiKeyUtils"

    fun hasChatGptKey(): Boolean {
        if (BuildConfig.DEBUG && System.getProperty("ALLOW_REPORTS_WITHOUT_KEYS") == "true") {
            return true
        }
        return BuildConfig.CHAT_GPT_API_KEY.isNotBlank()
    }

    fun hasGeminiKey(): Boolean = BuildConfig.GEMINI_API_KEY.isNotBlank()

    fun hasNlgKeys(): Boolean = hasChatGptKey() && hasGeminiKey()

    fun logMissingKeysIfAny() {
        if (!hasChatGptKey()) {
            Log.w(TAG, "CHAT_GPT_API_KEY is missing; ChatGPT features are disabled.")
        }
        if (!hasGeminiKey()) {
            Log.w(TAG, "GEMINI_API_KEY is missing; Gemini features are disabled.")
        }
    }
}
