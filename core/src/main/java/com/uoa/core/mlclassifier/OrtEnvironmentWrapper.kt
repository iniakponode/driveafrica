package com.uoa.core.mlclassifier

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

class OrtEnvironmentWrapper {
    val ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()

    fun createSession(modelBytes: ByteArray): OrtSession {
        return ortEnvironment.createSession(modelBytes)
    }
}