package com.uoa.dbda.domain.usecase


import android.os.Build
import androidx.annotation.RequiresApi
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.model.UnsafeBehaviourModel
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class GetUnsafeBehavioursBetweenDatesUseCase @Inject constructor(
    private val unsafeBehaviourRepository: UnsafeBehaviourRepository
) {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun execute(startDate: Long, endDate: Long): Flow<List<UnsafeBehaviourModel>> {
        val zoneId = ZoneId.systemDefault()
        val sDate = Instant.ofEpochMilli(startDate).atZone(zoneId).toLocalDate()
        val eDate = Instant.ofEpochMilli(endDate).atZone(zoneId).toLocalDate()
        return unsafeBehaviourRepository.getUnsafeBehavioursBetweenDates(sDate, eDate)
    }
}
