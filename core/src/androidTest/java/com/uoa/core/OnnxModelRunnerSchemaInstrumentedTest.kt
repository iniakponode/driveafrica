package com.uoa.core

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.uoa.core.mlclassifier.OrtEnvironmentWrapper
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.TensorInfo
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class OnnxModelRunnerSchemaInstrumentedTest {

    @Test
    fun logsOutputSchema() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFileName = "bagging_classifier_with_probabilities.onnx"
        val modelFile = File(context.filesDir, modelFileName)
        if (!modelFile.exists()) {
            context.assets.open(modelFileName).use { input ->
                FileOutputStream(modelFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        val session = OrtEnvironmentWrapper().createSession(modelFile.absolutePath)
        val outputInfo = session.outputInfo
        val summary = buildString {
            append("outputNames=").append(session.outputNames).append('\n')
            append("inputNames=").append(session.inputNames).append('\n')
            outputInfo.forEach { (name, nodeInfo) ->
                val info = nodeInfo.info
                val detail = when (info) {
                    is TensorInfo -> {
                        val shape = info.shape?.contentToString() ?: "unknown"
                        "TensorInfo(type=${info.type}, shape=$shape)"
                    }
                    else -> info.toString()
                }
                append(name).append(": ").append(detail).append('\n')
            }
        }
        val outFile = File(context.filesDir, "onnx_schema.txt")
        outFile.writeText(summary)
        Log.i("OnnxSchemaTest", "Wrote schema to ${outFile.absolutePath}")

        val externalDir = context.getExternalFilesDir(null)
        if (externalDir != null) {
            val externalFile = File(externalDir, "onnx_schema.txt")
            externalFile.writeText(summary)
            Log.i("OnnxSchemaTest", "Wrote schema to ${externalFile.absolutePath}")
        }
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        summary.lineSequence().filter { it.isNotBlank() }.forEach { line ->
            val sanitized = line.replace("\"", "'")
            uiAutomation.executeShellCommand("log -t OnnxSchemaTest \"$sanitized\"")
        }

        val inputName = session.inputNames.firstOrNull() ?: error("No input names in ONNX model")
        val input = arrayOf(floatArrayOf(0f, 0f, 0f, 0f, 0f))
        OnnxTensor.createTensor(OrtEnvironmentWrapper().ortEnvironment, input).use { tensor ->
            session.run(mapOf(inputName to tensor)).use { outputs ->
                Log.i("OnnxSchemaTest", "Output count: ${outputs.size()}")
            }
        }
        session.close()
    }
}
