package com.example.aauapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aauapp.data.remote.BackendRepository
import com.example.aauapp.data.remote.BuildingMapDto
import com.example.aauapp.data.remote.CampusDto
import com.example.aauapp.data.remote.FloorMapDto
import com.example.aauapp.data.remote.FloorPlanRepository
import com.example.aauapp.data.remote.SpaceDisplayDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExploreUiState(
    val isLoading: Boolean = false,
    val campuses: List<CampusDto> = emptyList(),
    val selectedCampus: CampusDto? = null,
    val buildings: List<BuildingMapDto> = emptyList(),
    val selectedBuilding: BuildingMapDto? = null,
    val floors: List<FloorMapDto> = emptyList(),
    val selectedFloor: FloorMapDto? = null,
    val spaces: List<SpaceDisplayDto> = emptyList(),
    val error: String? = null
)

class ExploreViewModel : ViewModel() {

    private val repository = BackendRepository()
    private val floorRepository = FloorPlanRepository()

    private val _uiState = MutableStateFlow(ExploreUiState(isLoading = true))
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        loadCampuses()
    }

    fun loadCampuses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val campuses = repository.getCampuses()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    campuses = campuses,
                    selectedCampus = null,
                    selectedBuilding = null,
                    selectedFloor = null,
                    buildings = emptyList(),
                    floors = emptyList(),
                    spaces = emptyList(),
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load campuses"
                )
            }
        }
    }

    fun selectCampus(campus: CampusDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selectedCampus = campus,
                selectedBuilding = null,
                selectedFloor = null,
                buildings = emptyList(),
                floors = emptyList(),
                spaces = emptyList(),
                error = null
            )

            try {
                val campusMap = repository.getCampusMapLight(campus.id)
                val buildings = campusMap.campus.buildings

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedCampus = campus,
                    buildings = buildings,
                    selectedBuilding = null,
                    floors = emptyList(),
                    selectedFloor = null,
                    spaces = emptyList(),
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load campus map"
                )
            }
        }
    }

    fun selectBuilding(building: BuildingMapDto) {
        _uiState.value = _uiState.value.copy(
            selectedBuilding = building,
            floors = building.floors,
            selectedFloor = null,
            spaces = emptyList(),
            error = null
        )
    }

    fun selectFloor(floor: FloorMapDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selectedFloor = floor,
                spaces = emptyList(),
                error = null
            )

            try {
                val spaces = floorRepository.getFloorDisplay(floor.id)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedFloor = floor,
                    spaces = spaces,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load rooms"
                )
            }
        }
    }
}