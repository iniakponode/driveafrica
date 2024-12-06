package com.uoa.core.mlclassifier

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtSession
import com.uoa.core.mlclassifier.data.TripFeatures
import java.io.FileInputStream

import android.content.Context
import android.util.Log
import ai.onnxruntime.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnnxModelRunner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ortEnvironmentWrapper: OrtEnvironmentWrapper
) : AutoCloseable {

    private val session: OrtSession

    init {
        session = try {
            // Copy the model file from assets to internal storage
            val modelFileName = "pruned_decision_tree_model.with_runtime_opt.ort"
            val modelFile = copyAssetToFile(modelFileName)
            // Create the session using the model file path
            ortEnvironmentWrapper.createSession(modelFile.absolutePath)
        } catch (e: Exception) {
            Log.e("OnnxModelRunner", "Failed to initialize ONNX session: ${e.message}", e)
            throw e
        }
    }

    // Copy the model file from assets to a file in internal storage
    private fun copyAssetToFile(assetFileName: String): File {
        val file = File(context.filesDir, assetFileName)
        if (!file.exists()) {
            context.assets.open(assetFileName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        return file
    }

//    // Load the model file from assets
//    private fun String.loadModelFile(): ByteBuffer {
//        val assetManager = context.assets
//        return assetManager.openFd(this).use { assetFileDescriptor ->
//            val inputStream = assetFileDescriptor.createInputStream()
//            val length = assetFileDescriptor.length
//            val buffer = ByteBuffer.allocateDirect(length.toInt()).order(ByteOrder.nativeOrder())
//            val bytesRead = inputStream.channel.read(buffer)
//            if (bytesRead.toLong() != length) {
//                throw IllegalStateException("Failed to read the entire model file")
//            }
//            buffer.flip()
//            buffer
//        }
//    }

    fun runInference(features: TripFeatures): Boolean {
        try {
            // Prepare the input data as a 2D FloatArray (assuming model expects shape [1, 5])
            val inputData = arrayOf(
                floatArrayOf(
                    features.hourOfDayMean,
                    features.dayOfWeekMean,
                    features.speedStd,
                    features.courseStd,
                    features.accelerationYOriginalMean
                )
            )

            // Retrieve input and output names from the session
            val inputName = session.inputNames.firstOrNull()
                ?: throw IllegalStateException("Model has no input names")
            val outputName = session.outputNames.firstOrNull()
                ?: throw IllegalStateException("Model has no output names")

            // Prepare the input tensor
            val inputTensor = OnnxTensor.createTensor(
                ortEnvironmentWrapper.ortEnvironment,
                inputData
            )

            // Run inference
            inputTensor.use { tensor ->
                val output = session.run(mapOf(inputName to tensor))

                // Get the output result
                val result = output[outputName]
                    ?: throw IllegalStateException("Output not found: $outputName")

                // Determine the data type and handle accordingly
                val isAlcoholInfluenced = when (val outputValue = result.get().value) {
                    is FloatArray -> {
                        val prediction = outputValue[0]
                        Log.d("OnnxModelRunner", "Model Output (FloatArray): $prediction")
                        prediction > 0.5f
                    }
                    is LongArray -> {
                        val prediction = outputValue[0]
                        Log.d("OnnxModelRunner", "Model Output (LongArray): $prediction")
                        prediction == 1L
                    }
                    else -> {
                        throw IllegalStateException("Unexpected output type: ${outputValue::class.java.name}")
                    }
                }

                Log.d("OnnxModelRunner", "Is Alcohol Influenced: $isAlcoholInfluenced")
                return isAlcoholInfluenced
            }
        } catch (e: Exception) {
            Log.e("OnnxModelRunner", "Error during inference: ${e.message}", e)
            throw e
        }
    }

    override fun close() {
        try {
            session.close()
            // Do not close ortEnvironment here
        } catch (e: Exception) {
            Log.e("OnnxModelRunner", "Error during closure: ${e.message}", e)
        }
    }
}

