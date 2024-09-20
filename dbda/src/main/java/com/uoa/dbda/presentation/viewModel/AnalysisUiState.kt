package com.uoa.dbda.presentation.viewModel

import com.uoa.core.model.UnsafeBehaviourModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.UUID

data class AnalysisUiState(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val selectedTripId: UUID? = null,
    val analysisResults: Flow<List<UnsafeBehaviourModel>> = emptyFlow(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

