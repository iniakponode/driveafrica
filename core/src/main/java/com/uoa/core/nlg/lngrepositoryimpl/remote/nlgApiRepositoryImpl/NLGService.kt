//package com.uoa.nlgengine.data.nlgApiService
//
//import android.util.Log
//import com.google.ai.client.generativeai.GenerativeModel
//import com.google.ai.client.generativeai.type.BlockThreshold
//import com.google.ai.client.generativeai.type.HarmCategory
//import com.google.ai.client.generativeai.type.SafetySetting
//import com.google.ai.client.generativeai.type.generationConfig
//import com.uoa.core.network.apiservices.InAppApiService
//import com.uoa.core.network.RetrofitInstance
//import com.uoa.core.network.*
//import com.uoa.core.network.apiservices.ChatGPTApiService
//import com.uoa.core.network.apiservices.GeminiApiService
//import com.uoa.core.network.model.DrivingBehaviourResponse
//import com.uoa.core.network.model.GeminiResponse
//import com.uoa.core.network.model.Message
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import java.util.UUID
//
//class NLGService(private val chatGptApiKey: String, private val geminiApiKey: String, private val geminiSecret: String) {
//
//    private val inAppApiService: InAppApiService by lazy {
//        RetrofitInstance.getRetrofitInstance().create(InAppApiService::class.java)
//    }
//
//    private val chatGptApiService: ChatGPTApiService by lazy {
//        RetrofitInstance.getChatGPTRetrofitInstanceWithHeader(chatGptApiKey).create(
//            ChatGPTApiService::class.java)
//    }
//
//    private val geminiApiService: GeminiApiService by lazy {
//        RetrofitInstance.getGeminiRetrofitInstanceWithHeader(geminiApiKey, geminiSecret).create(
//            GeminiApiService::class.java)
//    }
//
//    fun getDrivingBehaviourReport(id: UUID, onResult: (String) -> Unit) {
//        inAppApiService.getDrivingBehaviour(id).enqueue(object : Callback<DrivingBehaviourResponse> {
//            override fun onResponse(call: Call<DrivingBehaviourResponse>, response: Response<DrivingBehaviourResponse>) {
//                if (response.isSuccessful) {
//                    val behaviourData = response.body()
//                    // Process data and generate report using GPT/Gemini APIs
////                    val report = generateReport(behaviourData)
////                    onResult(report)
//                } else {
//                    onResult("Failed to fetch data")
//                }
//            }
//
//            override fun onFailure(call: Call<DrivingBehaviourResponse>, t: Throwable) {
//                onResult("Error: ${t.message}")
//            }
//        })
//    }
//
//    private suspend fun generateReport(data: DrivingBehaviourResponse?): String {
//        // Implement your logic to process data and query GPT/Gemini APIs
//        // Example: return "Generated report based on data"
//
//        // Example of creating ChatCompletionRequest
//        val chatRequest = ChatCompletionRequest(
//            model = "gpt-3.5-turbo",
//            messages = listOf(
//                Message(role = "system", content = "You are a helpful assistant."),
//                Message(role = "user", content = "Generate a report based on the following data: $data")
//            ),
//            max_tokens = 100,
//            temperature = 0.7,
//            top_p = 0.95,
//            n = 1
//        )
//        chatGptApiService.getChatCompletion(chatRequest).enqueue(object : Callback<ChatCompletionResponse> {
//            override fun onResponse(call: Call<ChatCompletionResponse>, response: Response<ChatCompletionResponse>) {
//                if (response.isSuccessful) {
//                    val chatResponse = response.body()
//                    // Process chatResponse
//
//                }
//            }
//
//            override fun onFailure(call: Call<ChatCompletionResponse>, t: Throwable) {
//                // Handle failure
//                if(t.message != null) {
//                    Log.i("FetchReportError","Error: ${t.message}")
//                }
//            }
//        })
//
//        // Example of calling Gemini API
//        geminiApiService.getGeminiData().enqueue(object : Callback<GeminiResponse> {
//            override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
//                if (response.isSuccessful) {
//                    val geminiData = response.body()
//                    // Process geminiData
//                }
//            }
//
//            override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
//                // Handle failure
//            }
//        })
//
//        val generativeModel = GenerativeModel(
//            // The Gemini 1.5 models are versatile and work with most use cases
//            modelName = "gemini-1.5-flash",
//            // Access your API key as a Build Configuration variable
//            apiKey = com.uoa.nlgengine.BuildConfig.apikey,
//            generationConfig { // Configure the generation
//                maxOutputTokens = 200
//                temperature = 0.7f
//                topK= 50
//                topP = 0.95f
//            },
//            safetySettings = listOf(
//                SafetySetting(
//                    HarmCategory.HARASSMENT,
//                    BlockThreshold.MEDIUM_AND_ABOVE,
//                ),
//                SafetySetting(
//                    HarmCategory.HATE_SPEECH,
//                    BlockThreshold.MEDIUM_AND_ABOVE,
//                ),
//                SafetySetting(
//                    HarmCategory.SEXUALLY_EXPLICIT,
//                    BlockThreshold.MEDIUM_AND_ABOVE,
//                ),
//                SafetySetting(
//                    HarmCategory.DANGEROUS_CONTENT,
//                    BlockThreshold.MEDIUM_AND_ABOVE,
//                ),
//            )
//        )
//
//
//        val generatedText = generativeModel.generateContent("Generate a report based on the following data: $data")
//
//        return generatedText.functionResponse?.response?.getString("text") ?: "Failed to generate report"
//    }
//}