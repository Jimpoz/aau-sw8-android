package com.example.aauapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aauapp.data.remote.BackendRepository
import com.example.aauapp.data.remote.BuildingMapDto
import com.example.aauapp.data.remote.CampusDto
import com.example.aauapp.data.remote.FloorMapDto
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
    val error: String? = null
)

class ExploreViewModel : ViewModel() {

    private val repository = BackendRepository()

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
                val firstCampus = campuses.firstOrNull()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    campuses = campuses,
                    selectedCampus = firstCampus
                )

                firstCampus?.let { campus ->
                    selectCampus(campus)
                }
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
                buildings = emptyList(),
                floors = emptyList(),
                error = null
            )

            try {
                val campusMap = repository.getCampusMapLight(campus.id)
                val buildings = campusMap.campus.buildings
                val firstBuilding = buildings.firstOrNull()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedCampus = campus,
                    buildings = buildings,
                    selectedBuilding = firstBuilding,
                    floors = firstBuilding?.floors.orEmpty()
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
            floors = building.floors
        )
    }
}