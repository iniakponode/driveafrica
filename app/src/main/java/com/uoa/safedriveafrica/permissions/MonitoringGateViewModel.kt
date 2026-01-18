package com.uoa.safedriveafrica.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.database.repository.DriverProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MonitoringGateViewModel @Inject constructor(
    private val driverProfileRepository: DriverProfileRepository
) : ViewModel() {

    private val _profileReady = MutableStateFlow(false)
    val profileReady = _profileReady.asStateFlow()

    fun refresh(profileId: UUID?) {
        viewModelScope.launch(Dispatchers.IO) {
            val ready = if (profileId == null) {
                false
            } else {
                driverProfileRepository.getDriverProfileById(profileId) != null
            }
            _profileReady.value = ready
        }
    }
}
