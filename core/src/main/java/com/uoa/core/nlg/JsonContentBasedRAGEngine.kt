package com.uoa.core.nlg

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import com.uoa.core.database.entities.EmbeddingEntity
import com.uoa.core.nlg.repository.EmbeddingUtilsRepository
import com.uoa.core.nlg.utils.Tokenizer
import com.uoa.core.utils.EmbeddingUtils.deserializeEmbedding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.UUID

class JsonContentBasedRAGEngine(
    private val modelPath: String,
    private val ortEnvironment: OrtEnvironment,
    private val embeddingUtilsRepository: EmbeddingUtilsRepository
) {

    private var ortSession: OrtSession? = null
    private var tokenizer: Tokenizer? = null

    // Function to initialize the RAG engine
    // Initialize the model session and tokenizer once
    fun initialize(context: Context) {
        try {
            // Copy the model from assets to internal storage
            val modelFile = copyModelFromAssets(context, "minilm.onnx")
            // Create an ONNX session with the model file
            ortSession = ortEnvironment.createSession(modelFile.absolutePath)
            Log.d("RAGEngine", "ONNX session initialized")

            // Load tokenizer
            tokenizer = loadTokenizer(context)
            Log.d("RAGEngine", "Tokenizer initialized")
        } catch (e: Exception) {
            Log.e("RAGEngine", "Error initializing RAGEngine", e)
        }
    }

    fun close() {
        try {
            ortSession?.close()
            ortSession = null
            Log.d("RAGEngine", "ONNX session closed")
        } catch (e: Exception) {
            Log.e("RAGEngine", "Error closing ONNX session", e)
        }
    }

    // Function to copy the model file from assets to internal storage
    private fun copyModelFromAssets(context: Context, assetFileName: String): File {
        Log.d("RAGEngine", "Starting copyModelFromAssets with assetFileName: $assetFileName")
        val file = File(context.filesDir, assetFileName)
        if (!file.exists()) {
            try {
                context.assets.open(assetFileName).use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d("RAGEngine", "Model file copied to internal storage")
            } catch (e: Exception) {
                Log.e("RAGEngine", "Error copying model from assets", e)
            }
        } else {
            Log.d("RAGEngine", "Model file already exists at: ${file.absolutePath}")
        }
        return file
    }

    // Function to load JSON data from assets
    private fun loadOffencesJson(context: Context): JSONArray {
        val jsonString: String
        try {
            val inputStream: InputStream = context.assets.open("national_traffic_offences_laws_and_penalties.json")
            jsonString = inputStream.bufferedReader().use { it.readText() }
        } catch (ex: Exception) {
            Log.e("RAGEngine", "Error loading JSON data", ex)
            throw ex
        }
        return JSONArray(jsonString)
    }

    // Function to embed the content of the offences JSON
    suspend fun embedOffencesJson(context: Context) = withContext(Dispatchers.IO) {
        Log.d("RAGEngine", "Embedding content from offences JSON")

        val offencesArray = loadOffencesJson(context)
        for (i in 0 until offencesArray.length()) {
            val offenceObj = offencesArray.getJSONObject(i)

            // Generate text from the JSON object
            val content = generateOffenceContent(offenceObj)
            Log.d("RAGEngine", "Processing offence: ${offenceObj.getString("offence")}")

            // Generate embeddings
            val embedding = generateEmbeddingFromChunk(content)
            if (embedding.isNotEmpty()) {
                val embeddingEntity = EmbeddingEntity(
                    chunkId = UUID.randomUUID(),
                    chunkText = content,
                    embedding = serializeEmbedding(embedding),
                    sourceType = "national_traffic_offences_laws_and_penalties",
                    sourcePage = i,
                    createdAt = System.currentTimeMillis()
                )
                embeddingUtilsRepository.saveEmbedding(embeddingEntity)
                Log.d("RAGEngine", "Successfully embedded: ${offenceObj.getString("offence")}")
            } else {
                Log.e("RAGEngine", "Failed to generate embedding for: ${offenceObj.getString("offence")}")
            }
        }
    }

    // Function to generate a structured string from the JSON object
    private fun generateOffenceContent(offenceObj: JSONObject): String {
        return """
            Offence: ${offenceObj.getString("offence")}
            Meaning: ${offenceObj.getString("meaning")}
            Penalty: ${offenceObj.optString("penalty", "Not specified")}
            Fine: ${offenceObj.optString("fine", "Not specified")}
            Law: ${offenceObj.optString("law", "Not specified")}
            Source: ${offenceObj.optString("source", "Not specified")}
        """.trimIndent()
    }

    // Function to generate embeddings from text chunks
    fun generateEmbeddingFromChunk(chunk: String): FloatArray {
        if (tokenizer == null || ortSession == null) {
            Log.e("RAGEngine", "RAGEngine not initialized properly")
            return FloatArray(0)
        }

        val inputs = tokenizer!!.encode(chunk)
        val inputIdsTensorData = arrayOf(inputs.inputIds)
        val attentionMaskTensorData = arrayOf(inputs.attentionMask)

        val inputIdsTensor = OnnxTensor.createTensor(ortEnvironment, inputIdsTensorData)
        val attentionMaskTensor = OnnxTensor.createTensor(ortEnvironment, attentionMaskTensorData)

        val inputsMap = mapOf(
            "input_ids" to inputIdsTensor,
            "attention_mask" to attentionMaskTensor
        )

        return try {
            val results = ortSession!!.run(inputsMap)
            val outputTensor = results[0] as OnnxTensor
            val outputBuffer = outputTensor.floatBuffer
            val outputArray = FloatArray(outputBuffer.remaining())
            outputBuffer.get(outputArray)
            outputArray
        } catch (e: Exception) {
            Log.e("RAGEngine", "Error generating embedding", e)
            FloatArray(0)
        } finally {
            inputIdsTensor.close()
            attentionMaskTensor.close()
        }
    }

    // Function to load tokenizer
    private fun loadTokenizer(context: Context): Tokenizer {
        val vocab = mutableMapOf<String, Int>()
        val inputStream = context.assets.open("vocab.txt") // Your tokenizer vocab file
        inputStream.bufferedReader().useLines { lines ->
            lines.forEachIndexed { index, line ->
                vocab[line] = index
            }
        }
        return Tokenizer(vocab)
    }

    private fun serializeEmbedding(embedding: FloatArray): ByteArray {
        // Implementation of the logic to serialize the embeddings into ByteArray for saving in the database
        // Allocate a ByteBuffer with capacity to hold all floats (4 bytes per float)
        val byteBuffer = ByteBuffer.allocate(embedding.size * 4) // Float is 4 bytes in Java

        // Put each float into the byte buffer
        for (value in embedding) {
            byteBuffer.putFloat(value)
        }

        // Convert ByteBuffer to ByteArray and return
        return byteBuffer.array()
    }

    suspend fun findSimilarChunksInMemory(embeddings: FloatArray): List<Triple<String, String, Float>> = withContext(Dispatchers.IO) {
        Log.d("RAGEngine", "Starting findSimilarChunksInMemory")
        // Step 1: Retrieve stored embeddings from the database
        val storedEmbeddings = embeddingUtilsRepository.getAllEmbeddings()
        Log.d("RAGEngine", "Retrieved ${storedEmbeddings.size} stored embeddings")

        val similarChunks = mutableListOf<Triple<String, String, Float>>() // Triple of chunk text, sourceType, and similarity score

        // Step 2: Compute cosine similarity for each stored embedding
        storedEmbeddings.forEach { embeddingEntity ->
            val storedEmbedding = deserializeEmbedding(embeddingEntity.embedding)
            val similarity = computeCosineSimilarity(embeddings, storedEmbedding)
            // Store the chunk text, sourceType, and similarity score
            similarChunks.add(Triple(embeddingEntity.chunkText, embeddingEntity.sourceType, similarity))
        }

        // Step 3: Sort by similarity score in descending order
        val sortedChunks = similarChunks.sortedByDescending { it.third }
        Log.d("RAGEngine", "Found top similar chunks")

        // Step 4: Return the top chunks (adjust the number based on your needs, e.g., top 5)
        sortedChunks.take(5)
    }

    private fun computeCosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        // Ensure both vectors are of the same length
        if (vectorA.size != vectorB.size) {
            Log.e("RAGEngine", "Vectors must be of the same length for cosine similarity.")
            return 0f
        }

        // Compute dot product and magnitudes of the vectors
        var dotProduct = 0f
        var magnitudeA = 0f
        var magnitudeB = 0f

        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            magnitudeA += vectorA[i] * vectorA[i]
            magnitudeB += vectorB[i] * vectorB[i]
        }

        magnitudeA = kotlin.math.sqrt(magnitudeA)
        magnitudeB = kotlin.math.sqrt(magnitudeB)

        // Avoid division by zero
        return if (magnitudeA > 0 && magnitudeB > 0) {
            dotProduct / (magnitudeA * magnitudeB)
        } else {
            0f
        }
    }
}