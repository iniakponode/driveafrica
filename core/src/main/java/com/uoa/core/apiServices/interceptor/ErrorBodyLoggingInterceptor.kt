package com.uoa.core.apiServices.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import kotlin.math.min

class ErrorBodyLoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            val responseBody = response.body
            if (responseBody == null) {
                Log.e(TAG, "${request.method} ${request.url} -> HTTP ${response.code} (empty error body)")
            } else {
                val body = runCatching { response.peekBody(MAX_BYTES).string() }.getOrDefault("")
                if (body.isBlank()) {
                    Log.e(TAG, "${request.method} ${request.url} -> HTTP ${response.code} (empty error body)")
                } else {
                    logInChunks("${request.method} ${request.url} -> HTTP ${response.code}", body)
                }
            }
        }

        return response
    }

    private fun logInChunks(prefix: String, body: String) {
        if (body.length <= LOG_CHUNK_SIZE) {
            Log.e(TAG, "$prefix\n$body")
            return
        }

        Log.e(TAG, "$prefix (body length=${body.length})")
        var start = 0
        var chunkIndex = 1
        while (start < body.length) {
            val end = min(start + LOG_CHUNK_SIZE, body.length)
            Log.e(TAG, "chunk $chunkIndex: ${body.substring(start, end)}")
            start = end
            chunkIndex++
        }
    }

    private companion object {
        private const val TAG = "ApiErrorBody"
        private const val LOG_CHUNK_SIZE = 4000
        private const val MAX_BYTES = 1024 * 1024L
    }
}
