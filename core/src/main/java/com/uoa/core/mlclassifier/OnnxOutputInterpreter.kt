package com.uoa.core.mlclassifier

import com.uoa.core.mlclassifier.data.ModelInference
import kotlin.math.abs
import kotlin.math.exp

class OnnxOutputInterpreter(
    private val alcoholClassIndex: Int = 1,
    private val probabilityOutputNameHints: List<String> =
        listOf("prob", "probability", "probabilities", "output_probability", "score", "scores"),
    private val labelOutputNameHints: List<String> =
        listOf("label", "labels", "output_label", "predicted")
) {

    fun interpret(outputValues: Map<String, Any?>): ModelInference {
        val probabilityOutputValue =
            findOutputValueByName(outputValues, probabilityOutputNameHints)
        val labelOutputValue =
            findOutputValueByName(outputValues, labelOutputNameHints)

        var probabilities = extractProbabilities(probabilityOutputValue)
        var label = extractLabel(labelOutputValue)

        if (probabilities == null || label == null) {
            for (value in outputValues.values) {
                if (probabilities == null) {
                    probabilities = extractProbabilities(value)
                }
                if (label == null) {
                    label = extractLabel(value)
                }
                if (probabilities != null && label != null) {
                    break
                }
            }
        }

        if (label == null && probabilities == null) {
            throw IllegalStateException("No usable outputs found for inference")
        }

        val rawProbabilities = probabilities?.copyOf()
        val normalizedProbs = probabilities?.let { normalizeScoresIfNeeded(it) }
        val probability = normalizedProbs?.let { probs ->
            when {
                probs.isEmpty() -> null
                probs.size == 1 -> probs[0]
                probs.size > alcoholClassIndex -> probs[alcoholClassIndex]
                else -> null
            }
        }?.coerceIn(0.0f, 1.0f)

        val isAlcoholInfluenced = when {
            label != null -> label == alcoholClassIndex.toLong()
            normalizedProbs != null -> {
                probability?.let { it >= 0.5f } ?: run {
                    val maxIndex = normalizedProbs.indices.maxByOrNull { normalizedProbs[it] } ?: 0
                    maxIndex == alcoholClassIndex
                }
            }
            else -> false
        }

        return ModelInference(
            isAlcoholInfluenced = isAlcoholInfluenced,
            probability = probability,
            rawProbabilities = rawProbabilities,
            normalizedProbabilities = normalizedProbs,
            rawLabel = label
        )
    }

    private fun findOutputValueByName(
        outputs: Map<String, Any?>,
        nameHints: List<String>
    ): Any? {
        return outputs.entries.firstOrNull { (name, _) ->
            val lowerName = name.lowercase()
            nameHints.any { hint -> lowerName.contains(hint) }
        }?.value
    }

    private fun extractProbabilities(outputValue: Any?): FloatArray? {
        return when (outputValue) {
            is FloatArray -> outputValue
            is DoubleArray -> outputValue.map { it.toFloat() }.toFloatArray()
            is Array<*> -> {
                val first = outputValue.firstOrNull()
                when (first) {
                    is FloatArray -> first
                    is DoubleArray -> first.map { it.toFloat() }.toFloatArray()
                    is Map<*, *> -> mapToFloatArray(first)
                    is Number -> floatArrayOf(first.toFloat())
                    else -> null
                }
            }
            is List<*> -> {
                val first = outputValue.firstOrNull()
                when (first) {
                    is Map<*, *> -> mapToFloatArray(first)
                    is FloatArray -> first
                    is DoubleArray -> first.map { it.toFloat() }.toFloatArray()
                    is Number -> floatArrayOf(first.toFloat())
                    else -> null
                }
            }
            is Map<*, *> -> mapToFloatArray(outputValue)
            else -> null
        }
    }

    private fun extractLabel(outputValue: Any?): Long? {
        return when (outputValue) {
            is LongArray -> outputValue.firstOrNull()
            is IntArray -> outputValue.firstOrNull()?.toLong()
            is Long -> outputValue
            is Int -> outputValue.toLong()
            is Array<*> -> {
                val first = outputValue.firstOrNull()
                when (first) {
                    is LongArray -> first.firstOrNull()
                    is IntArray -> first.firstOrNull()?.toLong()
                    is Long -> first
                    is Int -> first.toLong()
                    is Number -> first.toLong()
                    else -> null
                }
            }
            is List<*> -> {
                val first = outputValue.firstOrNull()
                when (first) {
                    is Long -> first
                    is Int -> first.toLong()
                    is Number -> first.toLong()
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun mapToFloatArray(map: Map<*, *>): FloatArray? {
        val entries = map.mapNotNull { (key, value) ->
            val index = when (key) {
                is Number -> key.toInt()
                is String -> key.toIntOrNull()
                else -> null
            }
            val prob = (value as? Number)?.toFloat()
            if (index != null && prob != null) index to prob else null
        }

        if (entries.isEmpty()) {
            return null
        }

        val size = entries.maxOf { it.first } + 1
        val probs = FloatArray(size)
        entries.forEach { (index, prob) ->
            if (index in probs.indices) {
                probs[index] = prob
            }
        }
        return probs
    }

    private fun normalizeScoresIfNeeded(values: FloatArray): FloatArray {
        if (values.isEmpty()) {
            return values
        }

        val inRange = values.all { it in 0.0f..1.0f }
        if (values.size == 1) {
            return if (inRange) values else floatArrayOf(sigmoid(values[0]))
        }

        val sum = values.sum()
        val looksLikeProbabilities = inRange && abs(sum - 1.0f) <= 0.01f
        return if (looksLikeProbabilities) values else softmax(values)
    }

    private fun sigmoid(value: Float): Float {
        return (1.0f / (1.0f + exp(-value.toDouble()))).toFloat()
    }

    private fun softmax(values: FloatArray): FloatArray {
        val max = values.maxOrNull() ?: 0.0f
        val exps = values.map { exp((it - max).toDouble()).toFloat() }
        val sum = exps.sum().coerceAtLeast(1.0e-6f)
        return exps.map { it / sum }.toFloatArray()
    }
}
