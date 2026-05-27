package com.example.aauapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aauapp.data.remote.FloorDto
import com.example.aauapp.data.remote.FloorMapDto
import com.example.aauapp.data.remote.FloorPlanRepository
import com.example.aauapp.data.remote.NavigationResultDto
import com.example.aauapp.data.remote.RouteStepDto
import com.example.aauapp.data.remote.SpaceDisplayDto
import com.example.aauapp.data.remote.VisibleBuildingDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FloorPlanUiState(
    val isLoading: Boolean = false,
    val floorId: String? = null,
    val floorName: String? = null,
    val floorIndex: Int? = null,
    val currentCampusId: String? = null,
    val currentBuildingId: String? = null,
    val availableFloors: List<FloorMapDto> = emptyList(),
    val spaces: List<SpaceDisplayDto> = emptyList(),
    val filteredSpaces: List<SpaceDisplayDto> = emptyList(),
    val selectedSpaceId: String? = null,
    val pendingFocusSpaceId: String? = null,
    val forcedUserSpaceId: String? = null,
    val forcedLocationSource: String? = null,
    val forcedUserLatitude: Double? = null,
    val forcedUserLongitude: Double? = null,
    val routeSteps: List<RouteStepDto> = emptyList(),
    val routePolyline: List<List<Double>> = emptyList(),
    val routePolylinesByFloor: Map<Int, List<List<Double>>> = emptyMap(),
    val isNavigating: Boolean = false,
    val visibleBuildings: List<VisibleBuildingDto> = emptyList(),
    val manualFloorPinAt: Long? = null,

    val error: String? = null
)

class FloorPlanViewModel : ViewModel() {

    private val repository = FloorPlanRepository()

    private val _uiState = MutableStateFlow(FloorPlanUiState())
    val uiState: StateFlow<FloorPlanUiState> = _uiState.asStateFlow()

    fun loadDefaultFloorForBuilding(buildingId: String) {
        if (buildingId.isBlank()) return
        viewModelScope.launch {
            try {
                val floors = repository.getBuildingFloors(buildingId)
                val target = floors
                    .filter { (it.floor_index ?: 0) >= 0 }
                    .minByOrNull { it.floor_index ?: Int.MAX_VALUE }
                    ?: floors.minByOrNull { it.floor_index ?: Int.MAX_VALUE }
                target?.let { loadFloor(it.id) }
            } catch (_: Exception) {
                // No floors / offline
            }
        }
    }

    fun fetchVisibleBuildings() {
        viewModelScope.launch {
            try {
                val list = repository.getVisibleBuildings()
                _uiState.value = _uiState.value.copy(visibleBuildings = list)
            } catch (_: Exception) {
                // Offline / failed to load; we'll just have no buildings in the dropdown.
            }
        }
    }

    fun enterBuilding(buildingId: String) {
        if (buildingId.isBlank() || buildingId == _uiState.value.currentBuildingId) return
        loadDefaultFloorForBuilding(buildingId)
    }

    fun loadFloor(floorId: String, userInitiated: Boolean = false) {
        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                routeSteps = emptyList(),
                routePolyline = emptyList()
            )

            try {

                val floor: FloorDto =
                    repository.getFloor(floorId)

                val spaces: List<SpaceDisplayDto> =
                    repository.getFloorDisplay(floorId)

                val floors = buildingFloorsFor(floor.building_id)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    floorId = floor.id,
                    floorName = floor.display_name ?: floor.id,
                    floorIndex = floor.floor_index,
                    currentBuildingId = floor.building_id,
                    currentCampusId = spaces.firstNotNullOfOrNull { it.campus_id },
                    availableFloors = floors,

                    spaces = spaces,
                    filteredSpaces = spaces,

                    selectedSpaceId = null,
                    manualFloorPinAt = if (userInitiated) System.currentTimeMillis()
                        else _uiState.value.manualFloorPinAt,
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

    private suspend fun buildingFloorsFor(buildingId: String?): List<FloorMapDto> =
        if (buildingId.isNullOrBlank()) emptyList()
        else runCatching {
            repository.getBuildingFloors(buildingId)
                .sortedByDescending { it.floor_index ?: 0 }
        }.getOrDefault(emptyList())

    fun forceUserSpace(
        spaceId: String,
        floorId: String?,
        source: String = "camera",
        latitude: Double? = null,
        longitude: Double? = null,
    ) {
        val pinsManual = source == "camera"
        val needsFloorSwitch = !floorId.isNullOrBlank() && floorId != _uiState.value.floorId
        if (!needsFloorSwitch) {
            _uiState.value = _uiState.value.copy(
                forcedUserSpaceId = spaceId,
                forcedLocationSource = source,
                forcedUserLatitude = latitude,
                forcedUserLongitude = longitude,
                selectedSpaceId = spaceId,
                manualFloorPinAt = if (pinsManual) System.currentTimeMillis()
                    else _uiState.value.manualFloorPinAt,
                error = null
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val floor = repository.getFloor(floorId)
                val spaces = repository.getFloorDisplay(floorId)
                val floors = buildingFloorsFor(floor.building_id)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    floorId = floor.id,
                    floorName = floor.display_name ?: floor.id,
                    floorIndex = floor.floor_index,
                    currentBuildingId = floor.building_id,
                    currentCampusId = spaces.firstNotNullOfOrNull { it.campus_id },
                    availableFloors = floors,
                    spaces = spaces,
                    filteredSpaces = spaces,
                    forcedUserSpaceId = spaceId,
                    forcedLocationSource = source,
                    forcedUserLatitude = latitude,
                    forcedUserLongitude = longitude,
                    selectedSpaceId = spaceId,
                    manualFloorPinAt = if (pinsManual) System.currentTimeMillis()
                        else _uiState.value.manualFloorPinAt,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load floor for snapped location"
                )
            }
        }
    }

    fun openInMap(floorId: String?, spaceId: String?) {
        val current = _uiState.value
        if (!floorId.isNullOrBlank() && floorId != current.floorId) {
            viewModelScope.launch {
                _uiState.value = current.copy(
                    isLoading = true,
                    error = null,
                    routeSteps = emptyList(),
                    routePolyline = emptyList()
                )
                try {
                    val floor = repository.getFloor(floorId)
                    val spaces = repository.getFloorDisplay(floorId)
                    val floors = buildingFloorsFor(floor.building_id)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        floorId = floor.id,
                        floorName = floor.display_name ?: floor.id,
                        floorIndex = floor.floor_index,
                        currentBuildingId = floor.building_id,
                        currentCampusId = spaces.firstNotNullOfOrNull { it.campus_id },
                        availableFloors = floors,
                        spaces = spaces,
                        filteredSpaces = spaces,
                        selectedSpaceId = spaceId,
                        pendingFocusSpaceId = spaceId,
                        error = null
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to open in map"
                    )
                }
            }
        } else {
            _uiState.value = current.copy(
                selectedSpaceId = spaceId ?: current.selectedSpaceId,
                pendingFocusSpaceId = spaceId
            )
        }
    }

    fun startNavigation() {
        if (_uiState.value.routePolyline.isEmpty()) return
        _uiState.value = _uiState.value.copy(isNavigating = true)
    }

    fun stopNavigation() {
        _uiState.value = _uiState.value.copy(isNavigating = false)
    }

    fun rerouteFrom(fromSpaceId: String) {
        val state = _uiState.value
        if (!state.isNavigating) return
        val destination = state.selectedSpaceId ?: return
        if (fromSpaceId == destination) return
        viewModelScope.launch {
            try {
                val result = repository.navigate(
                    fromSpaceId = fromSpaceId,
                    toSpaceId = destination,
                    accessibleOnly = NavigationPreferenceMemory.wheelchairOnly
                )
                _uiState.value = _uiState.value.copy(
                    routeSteps = result.steps,
                    routePolyline = result.polyline,
                    routePolylinesByFloor = result.polylines_by_floor
                        .mapKeys { (k, _) -> k.toIntOrNull() ?: -999 }
                        .filterKeys { it != -999 }
                )
            } catch (_: Exception) {
                // Keep the existing route; we'll retry on the next off-route tick.
            }
        }
    }

    fun updateLiveLocation(spaceId: String) {
        _uiState.value = _uiState.value.copy(
            forcedUserSpaceId = spaceId,
            forcedLocationSource = "wifi",
            forcedUserLatitude = null,
            forcedUserLongitude = null,
        )
    }

    fun clearForcedLocation() {
        _uiState.value = _uiState.value.copy(
            forcedUserSpaceId = null,
            forcedLocationSource = null,
            forcedUserLatitude = null,
            forcedUserLongitude = null,
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedSpaceId = null,
            routeSteps = emptyList(),
            routePolyline = emptyList(),
            routePolylinesByFloor = emptyMap(),
            isNavigating = false
        )
    }

    fun clearFocus() {
        if (_uiState.value.pendingFocusSpaceId != null) {
            _uiState.value = _uiState.value.copy(pendingFocusSpaceId = null)
        }
    }

    fun selectSpace(spaceId: String) {

        _uiState.value = _uiState.value.copy(
            selectedSpaceId = spaceId,
            error = null
        )
    }

    fun searchSpaces(query: String) {

        val allSpaces = _uiState.value.spaces

        if (query.isBlank()) {

            _uiState.value = _uiState.value.copy(
                filteredSpaces = allSpaces
            )

            return
        }

        val filtered = allSpaces.filter {

            val name = it.display_name ?: ""
            val short = it.short_name ?: ""
            val type = it.space_type ?: ""

            name.contains(query, ignoreCase = true) ||
                    short.contains(query, ignoreCase = true) ||
                    type.contains(query, ignoreCase = true)
        }

        _uiState.value = _uiState.value.copy(
            filteredSpaces = filtered
        )
    }

    fun selectBestMatchingSpace(query: String) {

        val normalized =
            query.trim().lowercase()

        val match = _uiState.value.spaces.firstOrNull {

            val display =
                it.display_name
                    ?.lowercase()
                    ?: ""

            val short =
                it.short_name
                    ?.lowercase()
                    ?: ""

            display.contains(normalized) ||
                    short.contains(normalized)
        }

        match?.let {

            _uiState.value = _uiState.value.copy(
                selectedSpaceId = it.id
            )

            computeRouteToSelected()
        }
    }

    fun computeRouteToSelected() {
        val state = _uiState.value
        val spaces = state.spaces
        val destination = state.selectedSpaceId ?: return

        val forcedId = state.forcedUserSpaceId
        val start = when {
            !forcedId.isNullOrBlank() && forcedId != destination -> forcedId
            else -> spaces.firstOrNull {
                it.id != destination && it.is_navigable != false
            }?.id ?: return
        }

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {

                val result: NavigationResultDto =
                    repository.navigate(
                        fromSpaceId = start,
                        toSpaceId = destination,

                        accessibleOnly =
                            NavigationPreferenceMemory.wheelchairOnly
                    )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    routeSteps = result.steps,
                    routePolyline = result.polyline,
                    routePolylinesByFloor = result.polylines_by_floor
                        .mapKeys { (k, _) -> k.toIntOrNull() ?: -999 }
                        .filterKeys { it != -999 },
                    error = null
                )

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                        ?: "Failed to compute route"
                )
            }
        }
    }

    fun clearRoute() {

        _uiState.value = _uiState.value.copy(
            routeSteps = emptyList(),
            routePolyline = emptyList(),
            routePolylinesByFloor = emptyMap(),
            isNavigating = false
        )
    }
}