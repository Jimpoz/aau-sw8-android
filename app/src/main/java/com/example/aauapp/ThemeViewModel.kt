package com.example.aauapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val store: ThemePreferencesStore
) : ViewModel() {

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkModeState: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    val isDarkMode: Boolean
        get() = _isDarkMode.value

    init {
        viewModelScope.launch {
            store.isDarkModeFlow.collect { enabled ->
                _isDarkMode.value = enabled
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        viewModelScope.launch {
            store.setDarkMode(enabled)
        }
    }
}

class ThemeViewModelFactory(
    private val store: ThemePreferencesStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ThemeViewModel(store) as T
    }
}