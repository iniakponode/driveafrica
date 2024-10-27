package com.uoa.dbda.domain.usecase

import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.dbda.repository.UnsafeBehaviourRepositoryImpl

class InsertUnsafeBehaviourUseCase(private val repository: UnsafeBehaviourRepositoryImpl) {
    suspend fun execute(unsafeBehaviour: UnsafeBehaviourModel) {
        repository.insertUnsafeBehaviour(unsafeBehaviour)
    }
}