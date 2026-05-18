package com.example.aauapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aauapp.data.remote.FloorDto
import com.example.aauapp.data.remote.FloorPlanRepository
import com.example.aauapp.data.remote.NavigationResultDto
import com.example.aauapp.data.remote.RouteStepDto
import com.example.aauapp.data.remote.SpaceDisplayDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FloorPlanUiState(
    val isLoading: Boolean = false,
    val floorId: String? = null,
    val floorName: String? = null,
    val spaces: List<SpaceDisplayDto> = emptyList(),
    val filteredSpaces: List<SpaceDisplayDto> = emptyList(),
    val selectedSpaceId: String? = null,
    val routeSteps: List<RouteStepDto> = emptyList(),
    val routePolyline: List<List<Double>> = emptyList(),
    val error: String? = null
)

class FloorPlanViewModel : ViewModel() {

    private val repository = FloorPlanRepository()

    private val _uiState = MutableStateFlow(FloorPlanUiState())
    val uiState: StateFlow<FloorPlanUiState> = _uiState.asStateFlow()

    fun loadFloor(floorId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                routeSteps = emptyList(),
                routePolyline = emptyList()
            )

            try {
                val floor: FloorDto = repository.getFloor(floorId)
                val spaces: List<SpaceDisplayDto> = repository.getFloorDisplay(floorId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    floorId = floor.id,
                    floorName = floor.display_name ?: floor.id,
                    spaces = spaces,
                    filteredSpaces = spaces,
                    selectedSpaceId = spaces.firstOrNull()?.id,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load floor"
                )
            }
        }
    }

    fun selectSpace(spaceId: String) {
        _uiState.value = _uiState.value.copy(
            selectedSpaceId = spaceId,
            error = null
        )
    }

    fun computeRouteToSelected() {
        val spaces = _uiState.value.spaces
        val destination = _uiState.value.selectedSpaceId ?: return

        val start = spaces.firstOrNull {
            it.id != destination && it.is_navigable != false
        }?.id ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val result: NavigationResultDto = repository.navigate(
                    fromSpaceId = start,
                    toSpaceId = destination
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    routeSteps = result.steps,
                    routePolyline = result.polyline,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to compute route"
                )
            }
        }
    }
}