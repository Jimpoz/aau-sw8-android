package com.example.aauapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aauapp.data.remote.BackendRepository
import com.example.aauapp.data.remote.BuildingMapDto
import com.example.aauapp.data.remote.CampusDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExploreUiState(
    val isLoading: Boolean = false,
    val campuses: List<CampusDto> = emptyList(),
    val buildings: List<BuildingMapDto> = emptyList(),
    val selectedCampusId: String? = null,
    val error: String? = null
)

class ExploreViewModel : ViewModel() {

    private val repository = BackendRepository()

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        loadCampuses()
    }

    fun loadCampuses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val campuses = repository.getCampuses()
                val firstCampus = campuses.firstOrNull()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    campuses = campuses,
                    selectedCampusId = firstCampus?.id
                )

                firstCampus?.id?.let { loadCampusMap(it) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load campuses"
                )
            }
        }
    }

    fun loadCampusMap(campusId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selectedCampusId = campusId,
                error = null
            )

            try {
                val map = repository.getCampusMapLight(campusId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    buildings = map.campus.buildings
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load campus map"
                )
            }
        }
    }
}