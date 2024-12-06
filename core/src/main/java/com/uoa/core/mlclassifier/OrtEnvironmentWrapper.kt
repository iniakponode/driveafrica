package com.uoa.core.mlclassifier

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import android.util.Log
import java.nio.ByteBuffer

class OrtEnvironmentWrapper {
    val ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()

    fun createSession(modelFilePath: String): OrtSession {
        return try {
            val sessionOptions = OrtSession.SessionOptions()
            // Configure session options if needed
            ortEnvironment.createSession(modelFilePath, sessionOptions)
        } catch (e: OrtException) {
            Log.e("OrtEnvironmentWrapper", "Failed to create ONNX session: ${e.message}", e)
            throw e
        }
    }

    // Remove the close() method since OrtEnvironment should not be closed manually
}


