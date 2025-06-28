package com.uoa.sensor.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.database.daos.RoadDao
import com.uoa.core.database.repository.RoadRepository
import com.uoa.core.model.Road
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoadViewModel @Inject constructor(private val repository: RoadRepository) : ViewModel() {

    private val _nearbyRoads = MutableStateFlow<List<Road>>(emptyList())
    val nearbyRoads: StateFlow<List<Road>> get() = _nearbyRoads

    fun fetchNearbyRoads(latitude: Double, longitude: Double, radius: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val roads = repository.getNearByRoad(latitude, longitude, radius)
            _nearbyRoads.value = roads
        }
    }
}
