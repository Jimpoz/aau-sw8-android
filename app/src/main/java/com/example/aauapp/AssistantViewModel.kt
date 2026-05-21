package com.example.aauapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aauapp.data.remote.AssistantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MessageRole {
    USER,
    ASSISTANT
}

data class ChatMessage(
    val text: String,
    val role: MessageRole
)

class AssistantViewModel : ViewModel() {

    private val repository = AssistantRepository()

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage(
                text = "Hello 👋 I'm your virtual assistant. How can I help you today?",
                role = MessageRole.ASSISTANT
            )
        )
    )

    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun sendMessage(
        text: String,
        campusId: String = "campus-aau-cph",
        buildingId: String? = null,
        userLat: Double? = null,
        userLon: Double? = null
    ) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        _messages.value = _messages.value + ChatMessage(
            text = cleanText,
            role = MessageRole.USER
        )

        viewModelScope.launch {
            try {
                val answer = repository.sendMessage(
                    text = cleanText,
                    campusId = campusId,
                    buildingId = buildingId,
                    userLat = userLat,
                    userLon = userLon
                )

                _messages.value = _messages.value + ChatMessage(
                    text = answer,
                    role = MessageRole.ASSISTANT
                )
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(
                    text = "Backend error: ${e.message}",
                    role = MessageRole.ASSISTANT
                )
            }
        }
    }
}