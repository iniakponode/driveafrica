package com.uoa.core.apiServices.services.aiModellInputApiService

import com.uoa.core.database.entities.AIModelInputsEntity


interface AIModelInputApiRepository {
    fun insertAiModelInput(aiModelInput: AIModelInputsEntity)
}