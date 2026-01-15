package com.uoa.driverprofile.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.ai.client.generativeai.GenerativeModel
import com.uoa.core.database.repository.DriverProfileRepository
import com.uoa.core.database.repository.DrivingTipRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.nlg.repository.NLGEngineRepository
import com.uoa.core.utils.ApiKeyUtils
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import com.uoa.driverprofile.domain.usecase.GetUnsafeBehavioursForTipsUseCase
import com.uoa.driverprofile.utils.generateDrivingTipPair
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class DailyDrivingTipWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val drivingTipRepository: DrivingTipRepository,
    private val driverProfileRepository: DriverProfileRepository,
    private val getUnsafeBehavioursForTipsUseCase: GetUnsafeBehavioursForTipsUseCase,
    private val geminiCaller: GenerativeModel,
    private val nlgEngineRepository: NLGEngineRepository,
    private val tripRepository: TripDataRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val profileId = resolveProfileId()
            if (profileId == null) {
                Log.w("DailyDrivingTipWorker", "No driver profile found; skipping tip generation.")
                return@withContext Result.success()
            }

            val drivingTipsList = drivingTipRepository.fetchDrivingTipsByProfileId(profileId)
                .map { it.toDomainModel() }
            val today = LocalDate.now()
            val fourDaysAgo = today.minusDays(4)

            val gptTipToday = drivingTipsList
                .firstOrNull { it.llm == LLM_GPT && it.date == today }
            val geminiTipToday = drivingTipsList
                .firstOrNull { it.llm == LLM_GEMINI && it.date == today }

            val missingGpt = gptTipToday == null
            val missingGemini = geminiTipToday == null

            if (!missingGpt && !missingGemini) {
                Log.d("DailyDrivingTipWorker", "Tips already present for today.")
                return@withContext Result.success()
            }

            val tipsNotUsedRecently = drivingTipsList.filter { tip ->
                tip.date.isBefore(fourDaysAgo) || tip.date.isEqual(fourDaysAgo)
            }

            var gptFallback: com.uoa.core.model.DrivingTip? = null
            var geminiFallback: com.uoa.core.model.DrivingTip? = null

            if (missingGpt) {
                gptFallback = tipsNotUsedRecently
                    .filter { it.llm == LLM_GPT }
                    .randomOrNull()
                if (gptFallback != null) {
                    val updatedTip = gptFallback.copy(date = today)
                    drivingTipRepository.updateDrivingTip(updatedTip.toEntity())
                    Log.d("DailyDrivingTipWorker", "Reused GPT tip for today.")
                }
            }

            if (missingGemini) {
                geminiFallback = tipsNotUsedRecently
                    .filter { it.llm == LLM_GEMINI }
                    .randomOrNull()
                if (geminiFallback != null) {
                    val updatedTip = geminiFallback.copy(date = today)
                    drivingTipRepository.updateDrivingTip(updatedTip.toEntity())
                    Log.d("DailyDrivingTipWorker", "Reused Gemini tip for today.")
                }
            }

            val canGenerateGpt = ApiKeyUtils.hasChatGptKey()
            val canGenerateGemini = ApiKeyUtils.hasGeminiKey()
            if (!canGenerateGpt && !canGenerateGemini) {
                Log.w("DailyDrivingTipWorker", "API keys missing; skipping tip generation.")
                return@withContext Result.success()
            }

            val recentBehaviors = getUnsafeBehavioursForTipsUseCase.execute(profileId)
                .filter { it.driverProfileId == profileId }
            if (recentBehaviors.isEmpty()) {
                Log.w("DailyDrivingTipWorker", "No unsafe behaviors available for tip generation.")
                return@withContext Result.success()
            }

            val usedGemini = getUsedBehaviorTypes(today, LLM_GEMINI)
            val usedGpt = getUsedBehaviorTypes(today, LLM_GPT)
            val behaviorsToProcess = recentBehaviors
                .distinctBy { it.behaviorType }
                .filter { behavior ->
                    val needsGemini = canGenerateGemini && !usedGemini.contains(behavior.behaviorType)
                    val needsGpt = canGenerateGpt && !usedGpt.contains(behavior.behaviorType)
                    needsGemini || needsGpt
                }

            if (behaviorsToProcess.isEmpty()) {
                Log.d("DailyDrivingTipWorker", "All behaviors already have tips for today.")
                return@withContext Result.success()
            }

            behaviorsToProcess.forEach { behavior ->
                val shouldGenerateGemini = canGenerateGemini && !usedGemini.contains(behavior.behaviorType)
                val shouldGenerateGpt = canGenerateGpt && !usedGpt.contains(behavior.behaviorType)
                val tipsPair = generateDrivingTipPair(
                    applicationContext,
                    behavior,
                    profileId,
                    geminiCaller,
                    nlgEngineRepository,
                    tripRepository,
                    generateGemini = shouldGenerateGemini,
                    generateGpt = shouldGenerateGpt
                )

                if (tipsPair == null) {
                    Log.e("DailyDrivingTipWorker", "Failed to generate tips from LLMs.")
                    return@withContext Result.retry()
                }

                val (geminiTip, gptTip) = tipsPair
                val newGeminiTip = geminiTip?.copy(driverProfileId = profileId, date = today)
                val newGptTip = gptTip?.copy(driverProfileId = profileId, date = today)

                if (shouldGenerateGemini && newGeminiTip != null) {
                    drivingTipRepository.insertDrivingTip(newGeminiTip.toEntity())
                    markBehaviorUsed(today, LLM_GEMINI, behavior.behaviorType)
                }
                if (shouldGenerateGpt && newGptTip != null) {
                    drivingTipRepository.insertDrivingTip(newGptTip.toEntity())
                    markBehaviorUsed(today, LLM_GPT, behavior.behaviorType)
                }
            }

            Result.success()
        } catch (ce: CancellationException) {
            Log.w("DailyDrivingTipWorker", "Work cancelled", ce)
            throw ce
        } catch (e: Exception) {
            Log.e("DailyDrivingTipWorker", "Unexpected error during tip generation", e)
            Result.retry()
        }
    }

    private suspend fun resolveProfileId(): UUID? {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(DRIVER_PROFILE_ID, null)
        val storedUuid = stored?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (storedUuid != null) {
            return storedUuid
        }

        val syncedProfile = driverProfileRepository.getDriverProfileBySyncStatus(true).firstOrNull()
        val unsyncedProfile = driverProfileRepository.getDriverProfileBySyncStatus(false).firstOrNull()
        val anyProfile = driverProfileRepository.getAllDriverProfiles().firstOrNull()
        return (syncedProfile ?: unsyncedProfile ?: anyProfile)?.driverProfileId
    }

    private companion object {
        const val LLM_GEMINI = "gemini"
        const val LLM_GPT = "gpt-4-turbo"
        const val TIP_BEHAVIOR_KEY_PREFIX = "driving_tip_behavior_used"
    }

    private fun behaviorKey(llm: String, date: LocalDate): String {
        return "${TIP_BEHAVIOR_KEY_PREFIX}_${llm}_${date}"
    }

    private fun getUsedBehaviorTypes(date: LocalDate, llm: String): MutableSet<String> {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getStringSet(behaviorKey(llm, date), emptySet()).orEmpty()
        return stored.toMutableSet()
    }

    private fun markBehaviorUsed(date: LocalDate, llm: String, behaviorType: String) {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = getUsedBehaviorTypes(date, llm).apply { add(behaviorType) }
        prefs.edit().putStringSet(behaviorKey(llm, date), updated).apply()
    }
}
