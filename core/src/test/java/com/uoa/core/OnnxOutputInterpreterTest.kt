package com.uoa.core

import com.uoa.core.mlclassifier.OnnxOutputInterpreter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OnnxOutputInterpreterTest {

    private val interpreter = OnnxOutputInterpreter()

    @Test
    fun interpret_returnsInfluenced_whenLabelIsOne() {
        val outputs = mapOf(
            "label" to arrayOf(longArrayOf(1)),
            "probabilities" to arrayOf(floatArrayOf(0.2f, 0.8f))
        )

        val inference = interpreter.interpret(outputs)

        assertTrue(inference.isAlcoholInfluenced)
        assertEquals(0.8f, inference.probability ?: 0f, 0.0001f)
    }

    @Test
    fun interpret_usesProbabilities_whenLabelMissing() {
        val outputs = mapOf(
            "probabilities" to arrayOf(floatArrayOf(0.1f, 0.9f))
        )

        val inference = interpreter.interpret(outputs)

        assertTrue(inference.isAlcoholInfluenced)
        assertEquals(0.9f, inference.probability ?: 0f, 0.0001f)
    }

    @Test
    fun interpret_throwsWhenNoUsableOutputs() {
        val outputs = mapOf(
            "unknown" to "invalid"
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            interpreter.interpret(outputs)
        }

        assertEquals("No usable outputs found for inference", exception.message)
    }
}
