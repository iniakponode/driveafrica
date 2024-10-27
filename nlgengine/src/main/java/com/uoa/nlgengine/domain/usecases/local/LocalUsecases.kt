package com.uoa.nlgengine.domain.usecases.local

import android.util.Log
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.utils.toDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.LocalDate
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

//
//        unsafeBehaviourList.chunked(1000).forEach { chunk ->
//            Log.d("UnsafeBehaviourByTripId", "Unsafe Behaviours for trip $tripId: $chunk")
//        }
        Log.d("UnsafeBehaviourByTripId", "Number of unsafe behaviours for trip $tripId: ${unsafeBehaviourList.size}")

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