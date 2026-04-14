package com.example.aauapp

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FloorChip(
    val id: Int,
    val label: String
)

data class RoomUi(
    val id: String,
    val name: String,
    val position: Offset
)

class FloorPlanViewModel : ViewModel() {

    private val _availableFloors = MutableStateFlow(
        listOf(
            FloorChip(0, "L1"),
            FloorChip(1, "L2"),
            FloorChip(2, "L3"),
            FloorChip(3, "G")
        )
    )
    val availableFloors: StateFlow<List<FloorChip>> = _availableFloors.asStateFlow()

    private val _selectedFloor = MutableStateFlow(1)
    val selectedFloor: StateFlow<Int> = _selectedFloor.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _rooms = MutableStateFlow(
        listOf(
            RoomUi("cafeteria", "Cafeteria", Offset(260f, 240f)),
            RoomUi("room_a", "Room A", Offset(700f, 520f)),
            RoomUi("elevator", "Elevator", Offset(220f, 520f)),
            RoomUi("info", "Info Desk", Offset(470f, 700f)),
            RoomUi("lab", "Lab", Offset(760f, 280f))
        )
    )
    val rooms: StateFlow<List<RoomUi>> = _rooms.asStateFlow()

    private val _selectedRoom = MutableStateFlow<RoomUi?>(null)
    val selectedRoom: StateFlow<RoomUi?> = _selectedRoom.asStateFlow()

    private val _userPosition = MutableStateFlow(Offset(180f, 760f))
    val userPosition: StateFlow<Offset> = _userPosition.asStateFlow()

    private val _route = MutableStateFlow<List<Offset>>(emptyList())
    val route: StateFlow<List<Offset>> = _route.asStateFlow()

    private val _statusText = MutableStateFlow("Indoor localization available")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    fun updateSearchText(value: String) {
        _searchText.value = value
    }

    fun selectFloor(floorId: Int) {
        _selectedFloor.value = floorId
    }

    fun selectRoom(room: RoomUi) {
        _selectedRoom.value = room
        buildRouteTo(room)
    }

    fun clearSelection() {
        _selectedRoom.value = null
        _route.value = emptyList()
    }

    fun zoomIn() {
        // placeholder for future map zoom state
    }

    fun zoomOut() {
        // placeholder for future map zoom state
    }

    fun updateDetectedLocation(locationName: String) {
        val matchedRoom = _rooms.value.firstOrNull {
            it.name.equals(locationName, ignoreCase = true)
        }

        if (matchedRoom != null) {
            _userPosition.value = matchedRoom.position
            _statusText.value = "Detected near ${matchedRoom.name}"
        } else {
            _statusText.value = "Detected: $locationName"
        }
    }

    private fun buildRouteTo(room: RoomUi) {
        val start = _userPosition.value
        val mid = Offset((start.x + room.position.x) / 2f, start.y)
        val end = room.position

        _route.value = listOf(start, mid, end)
        _statusText.value = "Route ready to ${room.name}"
    }
}