//package com.uoa.core.network
//
//import com.google.gson.GsonBuilder
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//
//class RetrofitInstance {
//
//    companion object {
//        const val BASE_URL = "https://afternoon-sands-09358-f3e117e55365.herokuapp.com/"
//        private const val CHATGPT_BASE_URL = "https://api.openai.com/v1/"
//        private const val GEMINI_BASE_URL = "https://api.gemini.com/v1/"
//        private const val ROAD_ADDRESS_BASE_URL = "https://nominatim.openstreetmap.org/"
//
//        // Shared Gson instance
//        private val gson by lazy {
//            GsonBuilder().create()
//        }
//
//        // Function to create a new OkHttpClient.Builder with logging interceptor
//        private fun getHttpClientBuilder(): OkHttpClient.Builder {
//            return OkHttpClient.Builder()
//                .addInterceptor(HttpLoggingInterceptor().apply {
//                    level = HttpLoggingInterceptor.Level.BODY
//                })
//        }
//
//        // Function to get Retrofit instance with optional headers
//        fun getRetrofitInstance(
//            baseUrl: String,
//            headers: Map<String, String>? = null
//        ): Retrofit {
//            val clientBuilder = getHttpClientBuilder()
//
//            headers?.let {
//                clientBuilder.addInterceptor { chain ->
//                    val requestBuilder = chain.request().newBuilder()
//                    for ((key, value) in it) {
//                        requestBuilder.addHeader(key, value)
//                    }
//                    chain.proceed(requestBuilder.build())
//                }
//            }
//
//            val client = clientBuilder.build()
//
//            return Retrofit.Builder()
//                .baseUrl(baseUrl)
//                .addConverterFactory(GsonConverterFactory.create(gson))
//                .client(client)
//                .build()
//        }
//
//        // Retrofit instance for BASE_URL
//        fun getDefaultRetrofitInstance(): Retrofit {
//            return getRetrofitInstance(BASE_URL)
//        }
//
//        // Retrofit instance for ChatGPT with Authorization header
//        fun getChatGPTRetrofitInstance(apiKey: String): Retrofit {
//            val headers = mapOf("Authorization" to "Bearer $apiKey")
//            return getRetrofitInstance(CHATGPT_BASE_URL, headers)
//        }
//
//        // Retrofit instance for Gemini with API key and payload
//        fun getGeminiRetrofitInstance(apiKey: String, payload: String): Retrofit {
//            val headers = mapOf(
//                "X-GEMINI-APIKEY" to apiKey,
//                "X-GEMINI-PAYLOAD" to payload
//            )
//            return getRetrofitInstance(GEMINI_BASE_URL, headers)
//        }
//
//        // Retrofit instance for Road Address API
//        fun getRoadNameRetrofitInstance(): Retrofit {
//            return getRetrofitInstance(ROAD_ADDRESS_BASE_URL)
//        }
//
//        // Generic function to create a service interface implementation
//        inline fun <reified T> createService(
//            baseUrl: String = BASE_URL,
//            headers: Map<String, String>? = null
//        ): T {
//            val retrofit = getRetrofitInstance(baseUrl, headers)
//            return retrofit.create(T::class.java)
//        }
//    }
//}
