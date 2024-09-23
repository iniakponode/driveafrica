package com.uoa.driverprofile.presentation.viewmodel
import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.database.entities.DrivingTipEntity
import com.uoa.core.database.repository.DrivingTipRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.model.DrivingTip
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.network.model.chatGPT.Message
import com.uoa.core.network.model.chatGPT.RequestBody
import com.uoa.core.nlg.lngrepositoryimpl.NLGEngineRepository
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import com.uoa.driverprofile.R
import com.uoa.driverprofile.domain.usecase.GetDrivingTipByIdUseCase
import com.uoa.driverprofile.domain.usecase.GetDrivingTipByProfileIdUseCase
import dagger.hilt.android.internal.Contexts.getApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DrivingTipsViewModel @Inject constructor(
    private val getDrivingTipByProfileIdUseCase: GetDrivingTipByProfileIdUseCase,
    private val getDrivingTipByIdUseCase: GetDrivingTipByIdUseCase,
    private val drivingTipRepository: DrivingTipRepository,
    private val unsafeBehaviourRepository: UnsafeBehaviourRepository,
    private val nlgEngineRepository: NLGEngineRepository,
    application: Application
) : ViewModel() {

    private val _drivingTips = MutableLiveData<List<DrivingTip>>()
    val drivingTips: LiveData<List<DrivingTip>> get() = _drivingTips

    init {
        val context = application.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val profileIdString = prefs.getString(DRIVER_PROFILE_ID, null)
        if (profileIdString != null) {
            val profileId = UUID.fromString(profileIdString)
            fetchDrivingTips(context,profileId)
        }

    }

    suspend fun getDrivingTipsForDriver(profileId: UUID): Flow<List<DrivingTip>> {
        return getDrivingTipByProfileIdUseCase.execute(profileId)
    }

    suspend fun getDrivingTipById(drivingTipId: UUID): DrivingTip {
        return withContext(Dispatchers.IO) {
            getDrivingTipByIdUseCase.execute(drivingTipId)
        }
//        return getDrivingTipByIdUseCase.execute(drivingTipId)
    }

    private fun createAIPrompt(behaviors: List<UnsafeBehaviourModel>, contextData: String): String {
        val uniqueBehaviorTypes = behaviors.map { it.behaviorType }.distinct().joinToString(", ")

        return """
    You are an expert in Nigerian driving laws and safety practices. Using the information provided below, generate personalized driving tips for the following unsafe driving behaviors:

    **Unsafe Behavior:**
    - Types: $uniqueBehaviorTypes

    **Context Data:**
    $contextData

    **Instructions:**
    - Provide a concise title for the tip.
    - Explain the meaning and implications of the unsafe behavior.
    - Include any applicable penalties or laws.
    - Offer actionable advice to improve driving habits.
    - Format your response in JSON with the following keys: title, meaning, penalty, law, summaryTip.

    **Example Format:**
    {
        "title": "Your Tip Title",
        "meaning": "Explanation of the unsafe behavior.",
        "penalty": "Applicable penalties.",
        "law": "Relevant laws.",
        "summaryTip": "Actionable advice."
    }
    """.trimIndent()
    }

    private fun parseTipContent(driverProfileId: UUID, tipContent: String): DrivingTip {
        return try {
            val jsonObject = JSONObject(tipContent)

            DrivingTip(
                tipId = UUID.randomUUID(),
                title = jsonObject.getString("title"),
                meaning = jsonObject.getString("meaning"),
                penalty = jsonObject.optString("penalty"),
                law = jsonObject.optString("law"),
                hostility = "", // Add appropriate value if necessary
                summaryTip = jsonObject.getString("summaryTip"),
                date = LocalDate.now(),
                profileId = driverProfileId,
            )
        } catch (e: JSONException) {
            // Log or handle JSON parsing error
            // Return a default or null DrivingTip to handle the error gracefully
            DrivingTip(
                tipId = UUID.randomUUID(),
                title = "Unknown Tip",
                meaning = "There was an error generating this tip.",
                penalty = null,
                law = null,
                hostility = "",
                summaryTip = "Please try again later.",
                date = LocalDate.now(),
                profileId = driverProfileId
            )
        }
    }

    private fun fetchDrivingTips(context: Context, profileId: UUID) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            val cachedTips = drivingTipRepository.fetchDrivingTipsByDate(today).map { it.toDomainModel() }

            if (cachedTips.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    _drivingTips.value = cachedTips
                }
            } else {
                val recentBehaviors =
                    unsafeBehaviourRepository.getUnsafeBehavioursBetweenDates(today.minusDays(7), today).toList()

                val contextData = loadContextData(context) // Replace with actual context data retrieval logic

                val tips = recentBehaviors.mapNotNull { behavior ->
                    val prompt = createAIPrompt(behavior, contextData)
                    val requestBody = RequestBody(
                        model = "gpt-3.5-turbo",
                        messages = listOf(Message(role = "user", content = prompt)),
                        maxTokens = 200,
                        temperature = 0f
                    )
                    val aiResponse = nlgEngineRepository.sendChatGPTPrompt(requestBody)
                    parseTipContent(profileId, aiResponse.choices.firstOrNull()!!.message.content ?: "")
                }

                tips.forEach { tip ->
                    drivingTipRepository.insertDrivingTip(tip.toEntity())
                }

                withContext(Dispatchers.Main) {
                    _drivingTips.value = tips
                }
            }
        }
    }
        private fun loadContextData(context: Context): String {
        // Load the context data from the raw resource
        val inputStream = context.resources.openRawResource(com.uoa.driverprofile.R.raw.driving_laws)
        return inputStream.bufferedReader().use { it.readText() }
    }
}