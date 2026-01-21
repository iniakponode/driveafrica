package com.uoa.core.mlclassifier.data

data class ModelInference(
    val isAlcoholInfluenced: Boolean,
    val probability: Float?,
    val rawProbabilities: FloatArray? = null,
    val normalizedProbabilities: FloatArray? = null,
    val rawLabel: Long? = null
)
