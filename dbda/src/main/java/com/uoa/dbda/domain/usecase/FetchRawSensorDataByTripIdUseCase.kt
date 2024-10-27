package com.uoa.dbda.domain.usecase

import android.util.Log
import com.uoa.core.model.RawSensorData
import com.uoa.core.utils.toDomainModel
import com.uoa.dbda.repository.UnsafeBehaviourRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class FetchRawSensorDataByTripIdUseCase @Inject constructor(
    private val unsafeBehaviourRepositoryImpl: UnsafeBehaviourRepositoryImpl
) {
    suspend fun execute(tripId: UUID): Flow<List<RawSensorData>> {
        return withContext(Dispatchers.IO) {
            unsafeBehaviourRepositoryImpl.getSensorDataByTripId(tripId)
                .map { rawSensorDataList ->
                    Log.i("FetchRawSensorDataByTripIdUseCase", "Number of sensor data: ${rawSensorDataList.size}, tripId: $tripId")
                    rawSensorDataList.map { it.toDomainModel() }
                }
        }
    }
}