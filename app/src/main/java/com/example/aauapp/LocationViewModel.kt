package com.example.aauapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserLocationUiState())
    val uiState: StateFlow<UserLocationUiState> = _uiState.asStateFlow()

    fun refreshLocation() {
        viewModelScope.launch {
            _uiState.value = locationService.getCurrentLocation()
        }
    }
}

class LocationViewModelFactory(
    private val locationService: LocationService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LocationViewModel(locationService) as T
    }
}