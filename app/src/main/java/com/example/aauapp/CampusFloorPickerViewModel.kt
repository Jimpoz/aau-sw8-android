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
    val floors: List<FloorMapDto> = emptyList(),
    val selectedBuildingId: String? = null,
    val selectedFloorId: String? = null,
    val error: String? = null
)

class CampusFloorPickerViewModel : ViewModel() {

    private val repository = BackendRepository()

    private val _uiState = MutableStateFlow(CampusFloorPickerUiState())
    val uiState: StateFlow<CampusFloorPickerUiState> = _uiState.asStateFlow()

    fun loadCampus(campusId: String) {
        if (campusId.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val campusMap = repository.getCampusMapLight(campusId)

                val buildings: List<BuildingMapDto> = campusMap.campus.buildings
                val firstBuilding: BuildingMapDto? = buildings.firstOrNull()

                val floors: List<FloorMapDto> = firstBuilding?.floors.orEmpty()
                val firstFloor: FloorMapDto? = floors.firstOrNull()

                _uiState.value = CampusFloorPickerUiState(
                    isLoading = false,
                    campusName = campusMap.campus.name,
                    buildings = buildings,
                    floors = floors,
                    selectedBuildingId = firstBuilding?.id,
                    selectedFloorId = firstFloor?.id,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = CampusFloorPickerUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load campus"
                )
            }
        }
    }

    fun selectBuilding(buildingId: String) {
        val building: BuildingMapDto? =
            _uiState.value.buildings.firstOrNull { it.id == buildingId }

        val floors: List<FloorMapDto> = building?.floors.orEmpty()
        val firstFloor: FloorMapDto? = floors.firstOrNull()

        _uiState.value = _uiState.value.copy(
            selectedBuildingId = buildingId,
            floors = floors,
            selectedFloorId = firstFloor?.id
        )
    }

    fun selectFloor(floorId: String) {
        _uiState.value = _uiState.value.copy(
            selectedFloorId = floorId
        )
    }
}