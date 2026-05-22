package com.example.aauapp.data.remote

class AssistantRepository(
    private val api: BackendApi = ApiModule.backendApi
) {
    suspend fun sendMessage(
        text: String,
        campusId: String,
        buildingId: String? = null,
        userLat: Double? = null,
        userLon: Double? = null,
        floorIndex: Int? = null
    ): String {
        return api.chatWithAssistant(
            AssistantChatRequest(
                user_query = text,
                campus_id = campusId,
                building_id = buildingId,
                user_lat = userLat,
                user_lon = userLon,
                floor_index = floorIndex
            )
        ).answer
    }
}