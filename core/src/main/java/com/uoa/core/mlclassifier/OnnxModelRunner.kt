package com.uoa.core.mlclassifier

import ai.onnxruntime.OnnxTensor
import com.uoa.core.mlclassifier.data.ModelInference
import com.uoa.core.mlclassifier.data.TripFeatures
import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxValue
import ai.onnxruntime.OrtSession
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnnxModelRunner private constructor(
    private val context: Context?,
    private val ortEnvironmentWrapper: OrtEnvironmentWrapper,
    private val sessionOverride: OrtSession?
) : AutoCloseable {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        ortEnvironmentWrapper: OrtEnvironmentWrapper
    ) : this(context, ortEnvironmentWrapper, null)

    @VisibleForTesting
    constructor(
        ortEnvironmentWrapper: OrtEnvironmentWrapper,
        session: OrtSession
    ) : this(null, ortEnvironmentWrapper, session)

    private val session: OrtSession
    private var outputSchemaLogged = false
    private val outputInterpreter = OnnxOutputInterpreter()

    init {
        session = sessionOverride ?: try {
            val appContext = requireNotNull(context) {
                "Context required to load ONNX model from assets."
            }
            // Copy the model file from assets to internal storage
            val modelFileName = "bagging_classifier_with_probabilities.onnx"
            val modelFile = copyAssetToFile(appContext, modelFileName)
            // Create the session using the model file path
            ortEnvironmentWrapper.createSession(modelFile.absolutePath)
        } catch (e: Exception) {
            Log.e("OnnxModelRunner", "Failed to initialize ONNX session: ${e.message}", e)
            throw e
        }
    }

    // Copy the model file from assets to a file in internal storage
    private fun copyAssetToFile(context: Context, assetFileName: String): File {
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

    fun runInference(features: TripFeatures): ModelInference {
        try {
            // Prepare the input data as a 2D FloatArray (assuming model expects shape [1, 5])
            val inputData = arrayOf(
                // Model expects: day_of_week_mean, hour_of_day_mean, accelerationYOriginal_mean, course_std, speed_std.
                floatArrayOf(
                    features.dayOfWeekMean,
                    features.hourOfDayMean,
                    features.accelerationYOriginalMean,
                    features.courseStd,
                    features.speedStd
                )
            )

            // Retrieve input and output names from the session
            val inputName = session.inputNames.firstOrNull { it == "float_input" }
                ?: session.inputNames.firstOrNull()
                ?: throw IllegalStateException("Model has no input names")
            if (session.outputNames.isEmpty()) {
                throw IllegalStateException("Model has no output names")
            }

            // Prepare the input tensor
            val inputTensor = OnnxTensor.createTensor(
                ortEnvironmentWrapper.ortEnvironment,
                inputData
            )

            inputTensor.use { tensor ->
                session.run(mapOf(inputName to tensor)).use { output ->
                    val outputValues = mutableMapOf<String, Any?>()
                    session.outputNames.forEachIndexed { index, outputName ->
                        outputValues[outputName] = extractOutputValue(output, outputName, index)
                    }
                    if (!outputSchemaLogged) {
                        logOutputSchema(outputValues)
                        outputSchemaLogged = true
                    }

                    val inference = outputInterpreter.interpret(outputValues)
                    Log.d(
                        "OnnxModelRunner",
                        "Inference prob=${inference.probability} influenced=${inference.isAlcoholInfluenced}"
                    )
                    return inference
                }
            }
        } catch (e: Exception) {
            Log.e("OnnxModelRunner", "Error during inference: ${e.message}", e)
            throw e
        }
    }

    private fun extractOutputValue(
        output: OrtSession.Result,
        outputName: String,
        index: Int
    ): Any? {
        val byIndex = runCatching { output[index] }.getOrNull()
        val byName = if (byIndex == null) {
            runCatching { output.get(outputName) }.getOrNull()
        } else {
            null
        }
        val onnxValue = (byIndex ?: byName)
        return when (onnxValue) {
            is OnnxValue -> onnxValue.value
            else -> onnxValue
        }
    }

    private fun logOutputSchema(outputs: Map<String, Any?>) {
        outputs.forEach { (name, value) ->
            Log.i("OnnxModelRunner", "Model output '$name' -> ${describeOutputValue(value)}")
        }
    }

    private fun describeOutputValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is FloatArray -> "FloatArray(size=${value.size})"
            is DoubleArray -> "DoubleArray(size=${value.size})"
            is LongArray -> "LongArray(size=${value.size})"
            is IntArray -> "IntArray(size=${value.size})"
            is Array<*> -> "Array(size=${value.size}, first=${value.firstOrNull()?.javaClass?.simpleName})"
            is Map<*, *> -> "Map(size=${value.size}, keys=${value.keys.take(3)})"
            else -> value.javaClass.simpleName
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

