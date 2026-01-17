package com.uoa.driverprofile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.apiServices.models.auth.FleetStatusResponse
import com.uoa.core.apiServices.services.fleetApiService.DriverFleetApiRepository
import com.uoa.core.network.NetworkMonitor
import com.uoa.core.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FleetStatusViewModel @Inject constructor(
    private val driverFleetApiRepository: DriverFleetApiRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _state = MutableStateFlow(FleetStatusUiState())
    val state = _state.asStateFlow()

    fun refreshFleetStatus() {
        viewModelScope.launch {
            val online = runCatching { networkMonitor.isOnline.first() }.getOrDefault(false)
            if (!online) {
                _state.update { it.copy(isLoading = false, errorMessage = null) }
                return@launch
            }
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = driverFleetApiRepository.getFleetStatus()) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        fleetStatus = result.data,
                        errorMessage = null
                    )
                }
                is Resource.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                else -> Unit
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}

data class FleetStatusUiState(
    val isLoading: Boolean = false,
    val fleetStatus: FleetStatusResponse? = null,
    val errorMessage: String? = null
)
