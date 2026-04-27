package com.example.aauapp.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody

class BackendRepository(
    private val api: BackendApi = ApiModule.backendApi
) {

    suspend fun pingBackend(): HealthResponseDto {
        return api.pingBackend()
    }

    // 🔹 CAMPUSES
    suspend fun getCampuses(): List<CampusDto> {
        return api.getCampuses()
    }

    suspend fun getCampusMapLight(campusId: String): CampusMapLightDto {
        return api.getCampusMapLight(campusId)
    }

    // 🔹 BUILDINGS → FLOORS
    suspend fun getBuildingFloors(buildingId: String): List<FloorMapDto> {
        return api.getBuildingFloors(buildingId)
    }

    // 🔹 FLOOR DATA
    suspend fun getFloor(floorId: String): FloorDto {
        return api.getFloor(floorId)
    }

    suspend fun getFloorDisplay(floorId: String): List<SpaceDisplayDto> {
        return api.getFloorDisplay(floorId)
    }

    // 🔹 NAVIGATION
    suspend fun navigate(
        fromSpaceId: String,
        toSpaceId: String,
        accessibleOnly: Boolean = false
    ): NavigationResultDto {
        return api.navigate(
            fromSpaceId = fromSpaceId,
            toSpaceId = toSpaceId,
            accessibleOnly = accessibleOnly
        )
    }

    // 🔹 ASSISTANT
    suspend fun chat(
        query: String,
        campusId: String
    ): AssistantChatResponse {
        return api.chatWithAssistant(
            AssistantChatRequest(
                user_query = query,
                campus_id = campusId
            )
        )
    }

    // 🔹 ROOM UPLOAD
    suspend fun getRoomNames(): RoomNamesResponseDto {
        return api.getRoomSummaryRooms()
    }

    suspend fun uploadRoomPhotos(
        roomName: RequestBody,
        north: MultipartBody.Part,
        east: MultipartBody.Part,
        south: MultipartBody.Part,
        west: MultipartBody.Part
    ): RoomObjectSetupResponseDto {
        return api.uploadRoomPhotos(
            roomName,
            north,
            east,
            south,
            west
        )
    }
}