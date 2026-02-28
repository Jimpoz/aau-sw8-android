package com.example.aauapp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AssistantViewModel : ViewModel() {

    // StateFlow stores messages
    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages

    // Function to add new message
    fun sendMessage(text: String) {
        _messages.value = _messages.value + text
    }
}