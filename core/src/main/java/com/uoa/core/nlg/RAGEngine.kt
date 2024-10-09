package com.uoa.core.nlg

import android.content.Context
import android.util.Base64
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.uoa.core.nlg.repository.EmbeddingUtilsRepository
import com.uoa.core.nlg.utils.Tokenizer
import com.uoa.core.utils.EmbeddingUtils.deserializeEmbedding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

class RAGEngine(
    private val modelPath: String,
    private val ortEnvironment: OrtEnvironment,
    private val embeddingUtilsRepository: EmbeddingUtilsRepository
) {

    // Cached instances
    private var ortSession: OrtSession? = null
    private var tokenizer: Tokenizer? = null

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

    // Function to extract text from a PDF resource
    suspend fun extractTextFromPdf(context: Context, pdfResId: Int): String = withContext(Dispatchers.IO) {
        Log.d("RAGEngine", "Starting extractTextFromPdf with pdfResId: $pdfResId")
        PDFBoxResourceLoader.init(context)
        val pdfStream: InputStream = context.resources.openRawResource(pdfResId)
        var document: PDDocument? = null
        try {
            document = PDDocument.load(pdfStream)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            Log.d("RAGEngine", "Extracted text length: ${text.length}")
            text
        } catch (e: Exception) {
            Log.e("RAGEngine", "Error extracting text from PDF", e)
            ""
        } finally {
            try {
                document?.close()
                pdfStream.close()
            } catch (e: Exception) {
                Log.e("RAGEngine", "Error closing PDF resources", e)
            }
        }
    }

    fun chunkText(text: String, maxChunkSize: Int = 500): List<String> {
        Log.d("RAGEngine", "Starting chunkText with maxChunkSize: $maxChunkSize")
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()

        for (sentence in sentences) {
            if (currentChunk.length + sentence.length <= maxChunkSize) {
                currentChunk.append(sentence).append(" ")
            } else {
                chunks.add(currentChunk.toString().trim())
                currentChunk = StringBuilder(sentence).append(" ")
            }
        }
        if (currentChunk.isNotEmpty()) chunks.add(currentChunk.toString().trim())
        Log.d("RAGEngine", "Chunked text into ${chunks.size} chunks")
        return chunks
    }

    suspend fun generateEmbeddingFromChunk(chunk: String): FloatArray = withContext(Dispatchers.IO) {
        Log.d("RAGEngine", "Starting generateEmbeddingFromChunk for chunk length: ${chunk.length}")
        if (tokenizer == null || ortSession == null) {
            Log.e("RAGEngine", "RAGEngine not initialized properly")
            return@withContext FloatArray(0)
        }

        val inputs = tokenizer!!.encode(chunk)
        Log.d("RAGEngine", "Tokenized input: inputIds length: ${inputs.inputIds.size}, attentionMask length: ${inputs.attentionMask.size}")

        // Add batch dimension: [1, sequence_length]
        val inputIdsTensorData = arrayOf(inputs.inputIds)
        val attentionMaskTensorData = arrayOf(inputs.attentionMask)

        var inputIdsTensor: OnnxTensor? = null
        var attentionMaskTensor: OnnxTensor? = null
        var outputTensor: OnnxTensor? = null
        try {
            // Create input tensors
            inputIdsTensor = OnnxTensor.createTensor(ortEnvironment, inputIdsTensorData)
            attentionMaskTensor = OnnxTensor.createTensor(ortEnvironment, attentionMaskTensorData)
            Log.d("RAGEngine", "Input tensors created")

            // Prepare the inputs map
            val inputsMap = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor
            )

            // Run the session
            val results = ortSession!!.run(inputsMap)
            outputTensor = results[0] as OnnxTensor
            Log.d("RAGEngine", "ONNX session run completed")

            val outputBuffer = outputTensor.floatBuffer
            val outputArray = FloatArray(outputBuffer.remaining())
            outputBuffer.get(outputArray)
            Log.d("RAGEngine", "Embedding generated, length: ${outputArray.size}")

            outputArray
        } catch (e: Exception) {
            Log.e("RAGEngine", "Error generating embedding from chunk", e)
            FloatArray(0)
        } finally {
            // Clean up
            try {
                inputIdsTensor?.close()
                attentionMaskTensor?.close()
                outputTensor?.close()
            } catch (e: Exception) {
                Log.e("RAGEngine", "Error closing ONNX resources", e)
            }
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

    private fun loadTokenizer(context: Context): Tokenizer? {
        Log.d("RAGEngine", "Starting loadTokenizer")
        val vocab = mutableMapOf<String, Int>()
        return try {
            // Load vocab.txt from the app's assets
            context.assets.open("vocab.txt").bufferedReader().useLines { lines ->
                lines.forEachIndexed { index, line ->
                    vocab[line] = index
                }
            }
            Log.d("RAGEngine", "Tokenizer loaded with vocab size: ${vocab.size}")
            Tokenizer(vocab)
        } catch (e: Exception) {
            Log.e("RAGEngine", "Error loading tokenizer", e)
            null
        }
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