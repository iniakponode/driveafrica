package com.uoa.core.utils

import java.nio.ByteBuffer

object EmbeddingUtils {

    // Function to serialize FloatArray to ByteArray
    fun serializeEmbedding(embedding: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(embedding.size * 4) // 4 bytes per float
        embedding.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    // Function to deserialize ByteArray back to FloatArray
    fun deserializeEmbedding(byteArray: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(byteArray)
        val floatArray = FloatArray(byteArray.size / 4) // 4 bytes per float
        buffer.asFloatBuffer().get(floatArray)
        return floatArray
    }
}
