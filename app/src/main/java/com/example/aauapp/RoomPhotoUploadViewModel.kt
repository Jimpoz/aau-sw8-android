package com.example.aauapp

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aauapp.data.remote.RoomSummaryRepository
import com.example.aauapp.data.remote.RoomListItemDto
import com.example.aauapp.data.remote.ViewSummaryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RoomPhotoUploadUiState(
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val rooms: List<RoomListItemDto> = emptyList(),
    val selectedRoomName: String = "",
    val northImage: Uri? = null,
    val eastImage: Uri? = null,
    val southImage: Uri? = null,
    val westImage: Uri? = null,
    val successMessage: String? = null,
    val roomObjects: Map<String, Int> = emptyMap(),
    val roomText: Map<String, Int> = emptyMap(),
    val storedViews: List<String> = emptyList(),
    val summaries: List<ViewSummaryDto> = emptyList(),
    val error: String? = null
)

class RoomPhotoUploadViewModel : ViewModel() {

    private val repository = RoomSummaryRepository()

    private val _uiState = MutableStateFlow(RoomPhotoUploadUiState(isLoading = true))
    val uiState: StateFlow<RoomPhotoUploadUiState> = _uiState.asStateFlow()

    init {
        loadRooms()
    }

    fun loadRooms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val rooms = repository.getRooms()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    rooms = rooms,
                    selectedRoomName = rooms.firstOrNull()?.room_name.orEmpty(),
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load room names"
                )
            }
        }
    }

    fun setRoomName(roomName: String) {
        _uiState.value = _uiState.value.copy(selectedRoomName = roomName)
    }

    fun setNorthImage(uri: Uri?) {
        _uiState.value = _uiState.value.copy(northImage = uri)
    }

    fun setEastImage(uri: Uri?) {
        _uiState.value = _uiState.value.copy(eastImage = uri)
    }

    fun setSouthImage(uri: Uri?) {
        _uiState.value = _uiState.value.copy(southImage = uri)
    }

    fun setWestImage(uri: Uri?) {
        _uiState.value = _uiState.value.copy(westImage = uri)
    }

    fun upload(contentResolver: ContentResolver) {
        val state = _uiState.value

        if (state.selectedRoomName.isBlank()) {
            _uiState.value = state.copy(error = "Please select or enter a room name")
            return
        }

        val north = state.northImage
        val east = state.eastImage
        val south = state.southImage
        val west = state.westImage

        if (north == null || east == null || south == null || west == null) {
            _uiState.value = state.copy(error = "Please choose all 4 images")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUploading = true,
                error = null,
                successMessage = null,
                roomObjects = emptyMap(),
                roomText = emptyMap(),
                storedViews = emptyList(),
                summaries = emptyList()
            )

            try {
                val response = repository.uploadRoomPhotos(
                    contentResolver = contentResolver,
                    roomName = state.selectedRoomName,
                    northUri = north,
                    eastUri = east,
                    southUri = south,
                    westUri = west
                )

                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    successMessage = "Upload completed for ${response.room_name}",
                    roomObjects = response.room_object_counts,
                    roomText = response.room_text_counts,
                    storedViews = response.stored_views,
                    summaries = response.room_summary,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = e.message ?: "Upload failed"
                )
            }
        }
    }
}