package com.uoa.core.di

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.GsonBuilder
import com.uoa.core.BuildConfig
import com.uoa.core.network.NetworkMonitorImpl
import com.uoa.core.network.apiservices.OSMApiService
import com.uoa.core.network.apiservices.ChatGPTApiService
import com.uoa.core.network.apiservices.GeminiApiService
import com.uoa.core.nlg.repository.NLGEngineRepository
import com.uoa.core.nlg.lngrepositoryimpl.remote.nlgApiRepositoryImpl.NLGEngineRepositoryImpl
import com.uoa.core.utils.internetconnectivity.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CHATGPT_BASE_URL = "https://api.openai.com/v1/"
    private const val GEMINI_BASE_UR = "https://api.gemini.com/v1/"
    private const val GEMINI_BASE_URL="https://generativelanguage.googleapis.com/"
    private const val ROAD_ADDRESS_BASE_URL = "https://nominatim.openstreetmap.org/"
    private const val DEFAULT_BASE_URL = "https://afternoon-sands-09358-f3e117e55365.herokuapp.com/"

    private val gson by lazy {
        GsonBuilder().create()
    }

    // Function to create a new OkHttpClient.Builder with logging interceptor
    private fun getHttpClientBuilder(): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // Increase connection timeout
            .writeTimeout(30, TimeUnit.SECONDS)   // Increase write timeout
            .readTimeout(30, TimeUnit.SECONDS)    // Increase read timeout
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
    }


    // Function to get Retrofit instance with optional headers
    private fun getRetrofitInstance(
        baseUrl: String,
        headers: Map<String, String>? = null
    ): Retrofit {
        val clientBuilder = getHttpClientBuilder()

        headers?.let {
            clientBuilder.addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                for ((key, value) in it) {
                    requestBuilder.addHeader(key, value)
                }
                chain.proceed(requestBuilder.build())
            }
        }

        val client = clientBuilder.build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()
    }

    // Provide ChatGPTApiService
    @Provides
    @Singleton
    fun provideChatGPTApiService(@ApplicationContext context: Context): ChatGPTApiService {
//        val apiKey = getApiKeyFromSecureStorage(context) // Implement this method securely

        val apiKey = BuildConfig.CHAT_GPT_API_KEY
        if (apiKey.isEmpty()) {
            throw IllegalStateException("ChatGPT API key not found. Please set the API key in local.properties.")
        }

        return getRetrofitInstance(
            baseUrl = CHATGPT_BASE_URL,
            headers = mapOf("Authorization" to "Bearer $apiKey")
        ).create(ChatGPTApiService::class.java)
    }

    // Provide GeminiApiService
    @Provides
    @Singleton
    fun provideGeminiApiService(@ApplicationContext context: Context): GeminiApiService {
//        val apiKey = getGeminiApiKey(context) // Implement this method securely
//        val payload = getGeminiPayload(context) // Implement this method securely

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty()) {
            throw IllegalStateException("Gemini API key not found. Please set the API key in local.properties.")
        }

        return getRetrofitInstance(
            baseUrl = GEMINI_BASE_URL,
            headers = mapOf("Authorization" to "Bearer $apiKey")
        ).create(GeminiApiService::class.java)
    }

    // Provide OSMApiService
    @Provides
    @Singleton
    fun provideOSMApiService(): OSMApiService {
        return getRetrofitInstance(
            baseUrl = ROAD_ADDRESS_BASE_URL
        ).create(OSMApiService::class.java)
    }

    @Provides
    fun provideGenerativeModel(): GenerativeModel {
        // Initialize and return your GenerativeModel instance here
        return GenerativeModel("gemini-1.5-flash", apiKey = BuildConfig.GEMINI_API_KEY) // Replace with your actual initialization
    }

    // Provide NLGEngineRepository
    @Provides
    @Singleton
    fun provideNLGEngineRepository(
        chatGPTApiService: ChatGPTApiService,
        geminiApiService: GeminiApiService,
        osmApiService: OSMApiService
    ): NLGEngineRepository {
        return NLGEngineRepositoryImpl(
            chatGPTApiService,
            geminiApiService,
            osmApiService
        )
    }


    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitorImpl(context)
    }
}