package com.uoa.core.mlclassifier.data

sealed class InferenceResult {
    data class Success(val alcoholInfluence: Boolean) : InferenceResult()
    data class Failure(val error: Throwable) : InferenceResult()
}