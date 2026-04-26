package com.example.aauapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aauapp.data.remote.BackendRepository
import com.example.aauapp.data.remote.BuildingMapDto
import com.example.aauapp.data.remote.FloorMapDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CampusFloorPickerUiState(
    val isLoading: Boolean = false,
    val campusName: String? = null,
    val buildings: List<BuildingMapDto> = emptyList(),
    val selectedBuildingId: String? = null,
    val floors: List<FloorMapDto> = emptyList(),
    val selectedFloorId: String? = null,
    val error: String? = null
)

class CampusFloorPickerViewModel : ViewModel() {

    private val repository = BackendRepository()

    private val _uiState = MutableStateFlow(CampusFloorPickerUiState())
    val uiState: StateFlow<CampusFloorPickerUiState> = _uiState.asStateFlow()

    fun loadCampus(campusId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val map = repository.getCampusMapLight(campusId)
                val buildings = map.campus.buildings
                val firstBuilding = buildings.firstOrNull()
                val floors = firstBuilding?.floors ?: emptyList()
                val firstFloor = floors.firstOrNull()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    campusName = map.campus.name,
                    buildings = buildings,
                    selectedBuildingId = firstBuilding?.id,
                    floors = floors,
                    selectedFloorId = firstFloor?.id,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.toString()
                )
            }
        }
    }

    fun selectBuilding(buildingId: String) {
        val building = _uiState.value.buildings.firstOrNull { it.id == buildingId }
        val floors = building?.floors ?: emptyList()
        _uiState.value = _uiState.value.copy(
            selectedBuildingId = buildingId,
            floors = floors,
            selectedFloorId = floors.firstOrNull()?.id
        )
    }

    fun selectFloor(floorId: String) {
        _uiState.value = _uiState.value.copy(selectedFloorId = floorId)
    }
}