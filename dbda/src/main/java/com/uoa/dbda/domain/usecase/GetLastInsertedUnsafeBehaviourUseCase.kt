package com.uoa.dbda.domain.usecase

import com.uoa.core.database.repository.UnsafeBehaviourRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

//class GetLastInsertedUnsafeBehaviourUseCase @Inject constructor(private val unsafeBehaviourRepository: UnsafeBehaviourRepository) {
//    // Add implementation here
//
//    suspend fun execute() {
//        // Add implementation here
//        return withContext(Dispatchers.IO) {
//            unsafeBehaviourRepository.getLastInsertedUnsafeBehaviour()
//        }
//    }
//
//}