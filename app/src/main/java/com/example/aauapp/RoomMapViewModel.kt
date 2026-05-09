package com.example.aauapp

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class UploadDirection(
    val label: String,
    val formField: String,
) {
    NORTH("North", "north_image"),
    EAST("East", "east_image"),
    SOUTH("South", "south_image"),
    WEST("West", "west_image"),
}

data class CampusUiModel(
    val id: String,
    val name: String,
)

data class MapPointUiModel(
    val x: Float,
    val y: Float,
)

data class MapSpaceUiModel(
    val id: String,
    val campusId: String,
    val campusName: String,
    val buildingId: String,
    val buildingName: String,
    val buildingShortName: String?,
    val floorId: String,
    val floorIndex: Int,
    val floorLabel: String,
    val displayName: String,
    val shortName: String?,
    val spaceType: String,
    val polygon: List<MapPointUiModel>,
    val centroidX: Float?,
    val centroidY: Float?,
    val areaM2: Float?,
    val tags: List<String>,
    val capacity: Int?,
    val isAccessible: Boolean?,
    val isNavigable: Boolean?,
    val isOutdoor: Boolean?,
)

data class MapFloorUiModel(
    val id: String,
    val campusId: String,
    val buildingId: String,
    val buildingName: String,
    val buildingShortName: String?,
    val floorIndex: Int,
    val label: String,
    val spaces: List<MapSpaceUiModel>,
)

data class RoomMapUiState(
    val campuses: List<CampusUiModel> = emptyList(),
    val selectedCampusId: String? = null,
    val selectedCampusName: String? = null,
    val floors: List<MapFloorUiModel> = emptyList(),
    val selectedFloorId: String? = null,
    val selectedSpace: MapSpaceUiModel? = null,
    val isLoadingCampuses: Boolean = false,
    val isLoadingMap: Boolean = false,
    val selectedUploadImages: Map<UploadDirection, Uri> = emptyMap(),
    val isUploadingRoomImages: Boolean = false,
    val uploadStatusMessage: String? = null,
    val uploadErrorMessage: String? = null,
    val lastUploadResult: RoomObjectDetectionSetupResponse? = null,
    val errorMessage: String? = null,
)

class RoomMapViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val apiService = RoomMapApiService()

    private val _uiState = MutableStateFlow(RoomMapUiState())
    val uiState: StateFlow<RoomMapUiState> = _uiState

    init {
        refreshRoomMap()
    }

    fun refreshRoomMap() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingCampuses = true,
                    isLoadingMap = true,
                    errorMessage = null,
                )
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    apiService.fetchCampuses()
                }
            }.onSuccess { campuses ->
                val campusItems = campuses.map { campus ->
                    CampusUiModel(
                        id = campus.id,
                        name = campus.name,
                    )
                }
                val selectedCampus = campusItems.firstOrNull()

                _uiState.update {
                    it.copy(
                        campuses = campusItems,
                        selectedCampusId = selectedCampus?.id,
                        selectedCampusName = selectedCampus?.name,
                        isLoadingCampuses = false,
                        errorMessage = if (campusItems.isEmpty()) {
                            "No campuses were returned from the gateway."
                        } else {
                            null
                        },
                    )
                }

                selectedCampus?.id?.let { campusId ->
                    loadCampusMap(campusId)
                } ?: _uiState.update {
                    it.copy(isLoadingMap = false)
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoadingCampuses = false,
                        isLoadingMap = false,
                        errorMessage = throwable.message ?: "Could not load campuses from the gateway.",
                    )
                }
            }
        }
    }

    fun selectCampus(campusId: String) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedCampusId = campusId,
                selectedCampusName = currentState.campuses.firstOrNull { it.id == campusId }?.name,
                isLoadingMap = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            loadCampusMap(campusId)
        }
    }

    fun selectFloor(floorId: String) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedFloorId = floorId,
                selectedSpace = null,
                selectedUploadImages = emptyMap(),
                isUploadingRoomImages = false,
                uploadStatusMessage = null,
                uploadErrorMessage = null,
                lastUploadResult = null,
            )
        }
    }

    fun selectSpace(space: MapSpaceUiModel) {
        _uiState.update {
            it.copy(
                selectedFloorId = space.floorId,
                selectedSpace = space,
                selectedUploadImages = emptyMap(),
                isUploadingRoomImages = false,
                uploadStatusMessage = null,
                uploadErrorMessage = null,
                lastUploadResult = null,
            )
        }
    }

    fun setUploadImage(direction: UploadDirection, uri: Uri) {
        _uiState.update {
            it.copy(
                selectedUploadImages = it.selectedUploadImages + (direction to uri),
                uploadStatusMessage = null,
                uploadErrorMessage = null,
                lastUploadResult = null,
            )
        }
    }

    fun clearUploadImage(direction: UploadDirection) {
        _uiState.update {
            it.copy(
                selectedUploadImages = it.selectedUploadImages - direction,
                uploadStatusMessage = null,
                uploadErrorMessage = null,
                lastUploadResult = null,
            )
        }
    }

    fun uploadSelectedSpaceImages() {
        val currentState = _uiState.value
        val selectedSpace = currentState.selectedSpace
        if (selectedSpace == null) {
            _uiState.update {
                it.copy(uploadErrorMessage = "Select a room before uploading images.")
            }
            return
        }

        val missingDirections = UploadDirection.entries.filterNot {
            currentState.selectedUploadImages.containsKey(it)
        }
        if (missingDirections.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    uploadErrorMessage = "Select all 4 directional images: North, East, South, West.",
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploadingRoomImages = true,
                    uploadStatusMessage = null,
                    uploadErrorMessage = null,
                    lastUploadResult = null,
                )
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    apiService.uploadRoomImages(
                        contentResolver = getApplication<Application>().contentResolver,
                        roomName = selectedSpace.displayName.ifBlank { selectedSpace.id },
                        imagesByDirection = currentState.selectedUploadImages,
                    )
                }
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isUploadingRoomImages = false,
                        uploadStatusMessage = "Room analysis uploaded for ${result.roomName}.",
                        uploadErrorMessage = null,
                        lastUploadResult = result,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isUploadingRoomImages = false,
                        uploadStatusMessage = null,
                        uploadErrorMessage = throwable.message ?: "Image upload failed.",
                        lastUploadResult = null,
                    )
                }
            }
        }
    }

    fun finishSuccessfulRoomAnalysis() {
        _uiState.update {
            it.copy(
                selectedSpace = null,
                selectedUploadImages = emptyMap(),
                isUploadingRoomImages = false,
                uploadStatusMessage = null,
                uploadErrorMessage = null,
                lastUploadResult = null,
            )
        }
    }

    private suspend fun loadCampusMap(campusId: String) {
        runCatching {
            withContext(Dispatchers.IO) {
                apiService.fetchCampusLightMap(campusId)
            }
        }.onSuccess { response ->
            val floors = response.campus.buildings.flatMap { building ->
                building.floors.map { floor ->
                    MapFloorUiModel(
                        id = "${building.id}:${floor.id}",
                        campusId = response.campus.id,
                        buildingId = building.id,
                        buildingName = building.name,
                        buildingShortName = building.shortName,
                        floorIndex = floor.floorIndex,
                        label = buildFloorLabel(building, floor),
                        spaces = floor.spaces
                            .filter { it.polygon.size >= 3 }
                            .map { space ->
                                MapSpaceUiModel(
                                    id = space.id,
                                    campusId = response.campus.id,
                                    campusName = response.campus.name,
                                    buildingId = building.id,
                                    buildingName = building.name,
                                    buildingShortName = building.shortName,
                                    floorId = "${building.id}:${floor.id}",
                                    floorIndex = floor.floorIndex,
                                    floorLabel = buildFloorLabel(building, floor),
                                    displayName = space.displayName ?: space.shortName ?: space.id,
                                    shortName = space.shortName,
                                    spaceType = space.spaceType ?: "UNKNOWN",
                                    polygon = space.polygon.mapNotNull { point ->
                                        if (point.size >= 2) {
                                            MapPointUiModel(
                                                x = point[0],
                                                y = point[1],
                                            )
                                        } else {
                                            null
                                        }
                                    },
                                    centroidX = space.centroidX,
                                    centroidY = space.centroidY,
                                    areaM2 = space.areaM2,
                                    tags = space.tags,
                                    capacity = space.capacity,
                                    isAccessible = space.isAccessible,
                                    isNavigable = space.isNavigable,
                                    isOutdoor = space.isOutdoor,
                                )
                            },
                    )
                }
            }.filter { it.spaces.isNotEmpty() }

            val firstFloor = floors.firstOrNull()
            _uiState.update {
                it.copy(
                    floors = floors,
                    selectedCampusId = response.campus.id,
                    selectedCampusName = response.campus.name,
                    selectedFloorId = firstFloor?.id,
                    selectedSpace = null,
                    isLoadingMap = false,
                    selectedUploadImages = emptyMap(),
                    isUploadingRoomImages = false,
                    uploadStatusMessage = null,
                    uploadErrorMessage = null,
                    lastUploadResult = null,
                    errorMessage = if (floors.isEmpty()) {
                        "The selected campus map does not contain drawable spaces."
                    } else {
                        null
                    },
                )
            }
        }.onFailure { throwable ->
            _uiState.update {
                it.copy(
                    floors = emptyList(),
                    selectedFloorId = null,
                    selectedSpace = null,
                    isLoadingMap = false,
                    selectedUploadImages = emptyMap(),
                    isUploadingRoomImages = false,
                    uploadStatusMessage = null,
                    uploadErrorMessage = null,
                    lastUploadResult = null,
                    errorMessage = throwable.message ?: "Could not load the campus map.",
                )
            }
        }
    }
}

private fun buildFloorLabel(
    building: MobileBuildingResponse,
    floor: MobileFloorResponse,
): String {
    val buildingLabel = building.shortName?.takeIf { it.isNotBlank() } ?: building.name
    val floorLabel = floor.displayName?.takeIf { it.isNotBlank() } ?: "Floor ${floor.floorIndex}"
    return "$buildingLabel - $floorLabel"
}