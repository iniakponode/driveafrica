package com.uoa.core.mlclassifier.data

sealed class InferenceResult {
    // "Success" can distinguish alcohol from no alcohol
    data class Success(
        val isAlcoholInfluenced: Boolean,
        val probability: Float?
    ) : InferenceResult()

    // If the model can’t infer, or there's no data, or a calculation led to NaN
    object NotEnoughData : InferenceResult()

    // If something goes wrong (exception, etc.)
    data class Failure(val error: Throwable) : InferenceResult()
}
