package com.example.aauapp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MessageRole {
    USER,
    ASSISTANT
}

data class ChatMessage(
    val text: String,
    val role: MessageRole
)

class AssistantViewModel : ViewModel() {

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage(
                text = "Hello 👋 I'm your virtual assistant. How can I help you today?",
                role = MessageRole.ASSISTANT
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun sendMessage(text: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        _messages.value = _messages.value + ChatMessage(
            text = cleanText,
            role = MessageRole.USER
        )

        _messages.value = _messages.value + ChatMessage(
            text = "Sorry, unfortunately I'm currently unavailable. Try again later!",
            role = MessageRole.ASSISTANT
        )
    }
}