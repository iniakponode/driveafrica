package com.uoa.driverprofile.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.apiServices.services.drivingTipApiService.DrivingTipApiRepository
import com.uoa.core.database.repository.DrivingTipRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.model.DrivingTip
import com.uoa.core.model.UnsafeBehaviourModel
import com.google.ai.client.generativeai.GenerativeModel
import com.uoa.core.nlg.repository.NLGEngineRepository
import com.uoa.core.notifications.VehicleNotificationManager
import com.uoa.core.utils.ApiKeyUtils
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.Resource
import com.uoa.core.utils.formatDateToUTCPlusOne
import com.uoa.core.utils.toDrivingTipCreate
import com.uoa.core.utils.toEntity
import com.uoa.driverprofile.domain.usecase.GetDrivingTipByIdUseCase
import com.uoa.driverprofile.domain.usecase.GetDrivingTipByProfileIdUseCase
import com.uoa.driverprofile.domain.usecase.GetUnsafeBehavioursForTipsUseCase
import com.uoa.driverprofile.utils.generateDrivingTipPair
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class DrivingTipsViewModel @Inject constructor(
    private val getDrivingTipByProfileIdUseCase: GetDrivingTipByProfileIdUseCase,
    private val getDrivingTipByIdUseCase: GetDrivingTipByIdUseCase,
    private val drivingTipRepository: DrivingTipRepository,
    private val drivingTipApiRepository: DrivingTipApiRepository,
    private val nlgEngineRepository: NLGEngineRepository,
    private val getUnsafeBehavioursForTipsUseCase: GetUnsafeBehavioursForTipsUseCase,
    private val geminiCaller: GenerativeModel,
    private val tripRepository: TripDataRepository,
    application: Application
) : ViewModel() {

    private val _gptDrivingTips = MutableLiveData<List<DrivingTip>>()
    val gptDrivingTips: LiveData<List<DrivingTip>> get() = _gptDrivingTips

    private val _geminiDrivingTips = MutableLiveData<List<DrivingTip>>()
    val geminiDrivingTips: LiveData<List<DrivingTip>> get() = _geminiDrivingTips

    private val _tipsLoading = MutableLiveData(false)
    val tipsLoading: LiveData<Boolean> get() = _tipsLoading

    private val appContext = application.applicationContext
    private val notificationManager = VehicleNotificationManager(appContext)

    private companion object {
        const val LLM_GEMINI = "gemini"
        const val LLM_GPT = "gpt-4-turbo"
        const val TIP_BEHAVIOR_KEY_PREFIX = "driving_tip_behavior_used"
        const val TIP_LAST_SHOWN_KEY_PREFIX = "driving_tip_last_shown"
    }

    val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedProfileId = prefs.getString(DRIVER_PROFILE_ID, null)

    private fun behaviorKey(llm: String, date: LocalDate): String {
        return "${TIP_BEHAVIOR_KEY_PREFIX}_${llm}_${date}"
    }

    private fun lastShownKey(llm: String, date: LocalDate): String {
        return "${TIP_LAST_SHOWN_KEY_PREFIX}_${llm}_${date}"
    }

    private fun getLastShownTipId(llm: String, date: LocalDate): UUID? {
        val stored = prefs.getString(lastShownKey(llm, date), null) ?: return null
        return runCatching { UUID.fromString(stored) }.getOrNull()
    }

    private fun setLastShownTipId(llm: String, date: LocalDate, tipId: UUID) {
        prefs.edit().putString(lastShownKey(llm, date), tipId.toString()).apply()
    }

    private fun getUsedBehaviorTypes(llm: String, date: LocalDate): MutableSet<String> {
        val key = behaviorKey(llm, date)
        val stored = prefs.getStringSet(key, emptySet()).orEmpty()
        return stored.toMutableSet()
    }

    private fun markBehaviorUsed(llm: String, date: LocalDate, behaviorType: String) {
        val key = behaviorKey(llm, date)
        val updated = getUsedBehaviorTypes(llm, date).apply { add(behaviorType) }
        prefs.edit().putStringSet(key, updated).apply()
    }

    private fun selectTipForDisplay(
        tips: List<DrivingTip>,
        llm: String,
        date: LocalDate
    ): DrivingTip? {
        if (tips.isEmpty()) {
            return null
        }
        val lastShown = getLastShownTipId(llm, date)
        val candidates = if (lastShown == null) {
            tips
        } else {
            tips.filter { it.tipId != lastShown }
        }
        val selected = candidates.randomOrNull() ?: return null
        setLastShownTipId(llm, date, selected.tipId)
        return selected
    }

    init {
        Log.d("DrivingTipsViewModel", "Initializing DrivingTipsViewModel")
        ApiKeyUtils.logMissingKeysIfAny()
        val profileIdString = prefs.getString(DRIVER_PROFILE_ID, null)
        if (profileIdString != null) {
            val profileId = UUID.fromString(profileIdString)
            Log.d("DrivingTipsViewModel", "Profile ID found: $profileId")
            fetchDrivingTips(appContext, profileId)
        } else {
            Log.d("DrivingTipsViewModel", "No profile ID found in shared preferences")
        }
    }

    fun refreshTips(profileId: UUID) {
        fetchDrivingTips(appContext, profileId)
    }

    private fun fetchDrivingTips(context: Context, profileId: UUID) {
        Log.d("DrivingTipsViewModel", "Starting fetchDrivingTips")
        viewModelScope.launch(Dispatchers.IO) {
            _tipsLoading.postValue(true)
            try {
                val drivingTipsList = getDrivingTipByProfileIdUseCase.execute(profileId)
                val today = LocalDate.now()
                val fourDaysAgo = today.minusDays(4)
                Log.d("DrivingTipsViewModel", "Fetched ${drivingTipsList.size} driving tips")

                val gptTipsToday = drivingTipsList
                    .filter { it.llm == LLM_GPT && it.date == today }
                val geminiTipsToday = drivingTipsList
                    .filter { it.llm == LLM_GEMINI && it.date == today }

                val selectedGpt = selectTipForDisplay(gptTipsToday, LLM_GPT, today)
                val selectedGemini = selectTipForDisplay(geminiTipsToday, LLM_GEMINI, today)

                val missingGpt = selectedGpt == null
                val missingGemini = selectedGemini == null

                _gptDrivingTips.postValue(selectedGpt?.let { listOf(it) } ?: emptyList())
                _geminiDrivingTips.postValue(selectedGemini?.let { listOf(it) } ?: emptyList())

                val tipsNotUsedRecently = drivingTipsList.filter { tip ->
                    tip.date.isBefore(fourDaysAgo) || tip.date.isEqual(fourDaysAgo)
                }
                Log.d("DrivingTipsViewModel", "Tips not used recently: ${tipsNotUsedRecently.size}")

                var gptFallback: DrivingTip? = null
                var geminiFallback: DrivingTip? = null

                if (missingGpt) {
                    gptFallback = tipsNotUsedRecently
                        .filter {
                            it.llm == LLM_GPT &&
                                it.tipId != getLastShownTipId(LLM_GPT, today)
                        }
                        .randomOrNull()
                    if (gptFallback != null) {
                        val updatedTip = gptFallback.copy(date = today)
                        drivingTipRepository.updateDrivingTip(updatedTip.toEntity())
                        setLastShownTipId(LLM_GPT, today, updatedTip.tipId)
                        _gptDrivingTips.postValue(listOf(updatedTip))
                        Log.d("DrivingTipsViewModel", "Updated GPT driving tip: $updatedTip")
                    }
                }

                if (missingGemini) {
                    geminiFallback = tipsNotUsedRecently
                        .filter {
                            it.llm == LLM_GEMINI &&
                                it.tipId != getLastShownTipId(LLM_GEMINI, today)
                        }
                        .randomOrNull()
                    if (geminiFallback != null) {
                        val updatedTip = geminiFallback.copy(date = today)
                        drivingTipRepository.updateDrivingTip(updatedTip.toEntity())
                        setLastShownTipId(LLM_GEMINI, today, updatedTip.tipId)
                        _geminiDrivingTips.postValue(listOf(updatedTip))
                        Log.d("DrivingTipsViewModel", "Updated Gemini driving tip: $updatedTip")
                    }
                }

                val canGenerateGpt = ApiKeyUtils.hasChatGptKey()
                val canGenerateGemini = ApiKeyUtils.hasGeminiKey()
                if (canGenerateGpt || canGenerateGemini) {
                    generateNewDrivingTip(
                        context,
                        profileId,
                        generateGemini = canGenerateGemini,
                        generateGpt = canGenerateGpt
                    )
                } else if (missingGpt && missingGemini) {
                    Log.w("DrivingTipsViewModel", "API keys missing; skipping tip generation.")
                }
            } catch (ce: CancellationException) {
                Log.w("DrivingTipsViewModel", "fetchDrivingTips cancelled", ce)
                throw ce
            } catch (e: Exception) {
                Log.e("DrivingTipsViewModel", "Error fetching driving tips", e)
            } finally {
                _tipsLoading.postValue(false)
            }
        }
    }

    private suspend fun generateNewDrivingTip(
        context: Context,
        profileId: UUID,
        generateGemini: Boolean,
        generateGpt: Boolean
    ) {
        if (!generateGemini && !generateGpt) {
            return
        }
        val recentBehaviors = getUnsafeBehavioursForTipsUseCase.execute(profileId)
            .filter { it.driverProfileId == profileId }
        Log.d("DrivingTipsViewModel", "Recent behaviors: $recentBehaviors")
        if (recentBehaviors.isNotEmpty()) {
            val today = LocalDate.now()
            val usedGemini = getUsedBehaviorTypes(LLM_GEMINI, today)
            val usedGpt = getUsedBehaviorTypes(LLM_GPT, today)

            val uniqueBehaviors = recentBehaviors.distinctBy { it.behaviorType }
            val behaviorsToProcess = uniqueBehaviors.filter { behavior ->
                val availableForGemini = generateGemini && !usedGemini.contains(behavior.behaviorType)
                val availableForGpt = generateGpt && !usedGpt.contains(behavior.behaviorType)
                availableForGemini || availableForGpt
            }

            if (behaviorsToProcess.isEmpty()) {
                Log.d("DrivingTipsViewModel", "All behaviors already have tips for today.")
                return
            }

            behaviorsToProcess.forEach { behavior ->
                Log.d("DrivingTipsViewModel", "Selected Behavior: ${behavior.behaviorType}")
                val shouldGenerateGemini = generateGemini && !usedGemini.contains(behavior.behaviorType)
                val shouldGenerateGpt = generateGpt && !usedGpt.contains(behavior.behaviorType)
                generateAndStoreDrivingTips(
                    context,
                    behavior,
                    profileId,
                    shouldGenerateGemini,
                    shouldGenerateGpt
                )
            }
        } else {
            Log.d("DrivingTipsViewModel", "No recent behaviors found")
        }
    }

    private suspend fun generateAndStoreDrivingTips(
        context: Context,
        unsafeBehavior: UnsafeBehaviourModel,
        profileId: UUID,
        generateGemini: Boolean,
        generateGpt: Boolean
    ) {
        try {
            val tipsPair = generateDrivingTipPair(
                context,
                unsafeBehavior,
                profileId,
                geminiCaller,
                nlgEngineRepository,
                tripRepository,
                generateGemini = generateGemini,
                generateGpt = generateGpt
            )
            if (tipsPair != null) {
                val (geminiTip, gptTip) = tipsPair
                val today = LocalDate.now()
                val newGeminiTip = geminiTip?.copy(driverProfileId = profileId, date = today)
                val newGptTip = gptTip?.copy(driverProfileId = profileId, date = today)
                if (generateGemini && newGeminiTip != null) {
                    drivingTipRepository.insertDrivingTip(newGeminiTip.toEntity())
                    markBehaviorUsed(LLM_GEMINI, today, unsafeBehavior.behaviorType)
                    setLastShownTipId(LLM_GEMINI, today, newGeminiTip.tipId)
                }
                if (generateGpt && newGptTip != null) {
                    drivingTipRepository.insertDrivingTip(newGptTip.toEntity())
                    markBehaviorUsed(LLM_GPT, today, unsafeBehavior.behaviorType)
                    setLastShownTipId(LLM_GPT, today, newGptTip.tipId)
                }
                withContext(Dispatchers.IO) {
                    if (generateGemini && newGeminiTip != null) {
                        val geminiFormattedDate = formatDateToUTCPlusOne(newGeminiTip.date)
                        val result = runCatching {
                            drivingTipApiRepository.createDrivingTip(
                                newGeminiTip.toDrivingTipCreate().copy(date = geminiFormattedDate)
                            )
                        }.getOrElse { error ->
                            Log.e("DrivingTipsViewModel", "Failed to upload Gemini tip", error)
                            notificationManager.displayNotification(
                                "Driving Tips",
                                "Gemini tip upload failed: ${error.localizedMessage ?: "Unknown error"}. Will retry."
                            )
                            null
                        }
                        if (result is Resource.Success) {
                            val syncedTip = newGeminiTip.copy(sync = true)
                            drivingTipRepository.updateDrivingTip(syncedTip.toEntity())
                        } else if (result is Resource.Error) {
                            Log.e("DrivingTipsViewModel", "Failed to upload Gemini tip: ${result.message}")
                            notificationManager.displayNotification(
                                "Driving Tips",
                                "Gemini tip upload failed: ${result.message ?: "Unknown error"}. Will retry."
                            )
                        }
                        _geminiDrivingTips.postValue(listOf(newGeminiTip))
                    }
                    if (generateGpt && newGptTip != null) {
                        val chatGptFormattedDate = formatDateToUTCPlusOne(newGptTip.date)
                        val result = runCatching {
                            drivingTipApiRepository.createDrivingTip(
                                newGptTip.toDrivingTipCreate().copy(date = chatGptFormattedDate)
                            )
                        }.getOrElse { error ->
                            Log.e("DrivingTipsViewModel", "Failed to upload GPT tip", error)
                            notificationManager.displayNotification(
                                "Driving Tips",
                                "GPT tip upload failed: ${error.localizedMessage ?: "Unknown error"}. Will retry."
                            )
                            null
                        }
                        if (result is Resource.Success) {
                            val syncedTip = newGptTip.copy(sync = true)
                            drivingTipRepository.updateDrivingTip(syncedTip.toEntity())
                        } else if (result is Resource.Error) {
                            Log.e("DrivingTipsViewModel", "Failed to upload GPT tip: ${result.message}")
                            notificationManager.displayNotification(
                                "Driving Tips",
                                "GPT tip upload failed: ${result.message ?: "Unknown error"}. Will retry."
                            )
                        }
                        _gptDrivingTips.postValue(listOf(newGptTip))
                    }
                    Log.d("DrivingTipsViewModel", "Driving tips generated and posted")
                }
            } else {
                Log.e("DrivingTipsViewModel", "Failed to generate driving tips")
            }
        } catch (ce: CancellationException) {
            Log.w("DrivingTipsViewModel", "generateAndStoreDrivingTips cancelled", ce)
            throw ce
        } catch (e: Exception) {
            Log.e("DrivingTipsViewModel", "Error generating and storing driving tips", e)
        }
    }

    suspend fun getDrivingTipById(drivingTipId: UUID): DrivingTip {
        Log.d("DrivingTipsViewModel", "Getting driving tip by ID: $drivingTipId")
        return withContext(Dispatchers.IO) {
            getDrivingTipByIdUseCase.execute(drivingTipId)
        }
    }
}
