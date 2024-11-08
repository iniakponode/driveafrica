package com.uoa.core.mlclassifier

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import java.nio.ByteBuffer
import java.io.FileInputStream

class OnnxModelRunner(private val context: Context, private val ortEnvironmentWrapper: OrtEnvironmentWrapper) {

    private lateinit var session: OrtSession

    init {
        try {
            // Load the ONNX model
            val modelBytes = "pruned_decision_tree_model.with_runtime_opt.ort".loadModelFile()
            session = ortEnvironmentWrapper.createSession(modelBytes.array())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Load the model file from assets
    private fun String.loadModelFile(): ByteBuffer {
        val assetManager = context.assets
        val inputStream = assetManager.open(this)
        val fileChannel = (inputStream as FileInputStream).channel
        val buffer = ByteBuffer.allocateDirect(fileChannel.size().toInt())
        fileChannel.read(buffer)
        buffer.flip()
        return buffer
    }

    // Function to run inference on the model
    fun runInference(hourOfDayMean: Float, dayOfWeekMean: Float, speedStd: Float, courseStd: Float, accelerationYOriginalMean: Float): Boolean {
        // Prepare the input data as a FloatArray with correct input shape
        val inputData = floatArrayOf(hourOfDayMean, dayOfWeekMean, speedStd, courseStd, accelerationYOriginalMean)

        // Prepare the input tensor
        val inputTensor = OnnxTensor.createTensor(ortEnvironmentWrapper.ortEnvironment, inputData)

        // Run inference
        val output = session.run(mapOf("input" to inputTensor))
        val result = output[0].value as Array<FloatArray> // Assuming the model output is a 2D array

        // Since it's a binary classification model, convert output to true/false
        val isAlcoholInfluenced = result[0][0] > 0.5 // Adjust threshold if needed
        Log.d("alcohol", "${result[0][0]}")
        Log.d("alcohol", "isAlcoholInfluenced: $isAlcoholInfluenced")
        return isAlcoholInfluenced
    }
}