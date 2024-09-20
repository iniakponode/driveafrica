package com.uoa.nlgengine.domain.usecases.local

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.collection.mutableFloatListOf
import com.uoa.core.database.entities.UnsafeBehaviourEntity
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.utils.toDomainModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

class UnsafeBehavioursBtwnDatesUseCase @Inject constructor(
    private val unsafeBehaviourRepository: UnsafeBehaviourRepository
) {
    suspend fun execute(sDate: LocalDate, eDate: LocalDate): List<UnsafeBehaviourModel> {
        val unsafeBehaviourList = unsafeBehaviourRepository.getUnsafeBehavioursBetweenDates(sDate, eDate)
            .firstOrNull() ?: emptyList()

        Log.d("ReportPeriod", "Unsafe Behaviours between $sDate and $eDate: $unsafeBehaviourList")
        return unsafeBehaviourList
    }


}



class UnsafeBehaviourByTripIdUseCase @Inject constructor(private val unsafeBehaviourRepository: UnsafeBehaviourRepository){
    suspend fun execute(tripId:UUID): List<UnsafeBehaviourModel>{
        val unsafeBehaviourList = unsafeBehaviourRepository.getUnsafeBehavioursByTripId(tripId)
            .firstOrNull() ?: emptyList()

        Log.d("UnsafeBehaviourByTripId", "Unsafe Behaviours for trip $tripId: $unsafeBehaviourList")
        return unsafeBehaviourList

    }
}

class GetLastInsertedUnsafeBehaviourUseCase @Inject constructor(private val unsafeBehaviourRepository: UnsafeBehaviourRepository) {
    suspend fun execute(): UnsafeBehaviourModel? {
        return withContext(Dispatchers.IO) {
            val unsafeBehaviourEntity = unsafeBehaviourRepository.getLastInsertedUnsafeBehaviour()
            unsafeBehaviourEntity?.toDomainModel()
        }
    }
}