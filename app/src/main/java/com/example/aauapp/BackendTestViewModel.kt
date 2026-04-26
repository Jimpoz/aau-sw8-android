package com.example.aauapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aauapp.data.remote.BackendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BackendTestUiState(
    val isLoading: Boolean = false,
    val result: String = "",
    val error: String? = null
)

class BackendTestViewModel : ViewModel() {

    private val repository = BackendRepository()

    private val _uiState = MutableStateFlow(BackendTestUiState())
    val uiState: StateFlow<BackendTestUiState> = _uiState.asStateFlow()

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = BackendTestUiState(isLoading = true)

            try {
                val response = repository.pingBackend()
                _uiState.value = BackendTestUiState(
                    isLoading = false,
                    result = "Health success:\nstatus = $response"
                )
            } catch (e: Exception) {
                _uiState.value = BackendTestUiState(
                    isLoading = false,
                    error = "Health failed: ${e.toString()}"
                )
            }
        }
    }

    fun testCampuses() {
        viewModelScope.launch {
            _uiState.value = BackendTestUiState(isLoading = true)

            try {
                val response = repository.getCampuses()

                val formatted = try {
                    "Campuses success:\n$response"
                } catch (e: Exception) {
                    "Error formatting response: ${e.message}"
                }


                _uiState.value = BackendTestUiState(
                    isLoading = false,
                    result = formatted
                )
            } catch (e: Exception) {
                _uiState.value = BackendTestUiState(
                    isLoading = false,
                    error = "Campuses failed: ${e.toString()}"
                )
            }
        }
    }
}