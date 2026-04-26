package com.example.aauapp.data.remote

class AssistantRepository(
    private val api: BackendApi = ApiModule.backendApi
) {
    suspend fun sendMessage(
        text: String,
        campusId: String
    ): String {
        return api.chatWithAssistant(
            AssistantChatRequest(
                user_query = text,
                campus_id = campusId
            )
        ).answer
    }
}