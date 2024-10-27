package com.uoa.dbda.domain.usecase


import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.utils.toDomainModel
import com.uoa.dbda.repository.UnsafeBehaviourRepositoryImpl
import javax.inject.Inject

class GetUnsafeBehavioursBySyncStatusUseCase @Inject constructor(
    private val repository: UnsafeBehaviourRepositoryImpl
) {
    suspend fun execute(synced: Boolean): List<UnsafeBehaviourModel> {
        val unsafeBehaviourModel= repository.getUnsafeBehavioursBySyncStatus(synced)
        return unsafeBehaviourModel.map { it.toDomainModel() }
    }
}
