package com.example.aauapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aauapp.data.remote.BackendRepository
import com.example.aauapp.data.remote.CampusDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CampusSelectionUiState(
    val isLoading: Boolean = false,
    val campuses: List<CampusDto> = emptyList(),
    val selectedCampus: CampusDto? = null,
    val error: String? = null
)

class CampusSelectionViewModel : ViewModel() {

    private val repository = BackendRepository()

    private val _uiState = MutableStateFlow(CampusSelectionUiState(isLoading = true))
    val uiState: StateFlow<CampusSelectionUiState> = _uiState.asStateFlow()

    init {
        loadCampuses()
    }

    fun loadCampuses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val campuses = repository.getCampuses()
                _uiState.value = CampusSelectionUiState(
                    isLoading = false,
                    campuses = campuses,
                    selectedCampus = campuses.firstOrNull(),
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = CampusSelectionUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load campuses"
                )
            }
        }
    }

    fun selectCampus(campus: CampusDto) {
        _uiState.value = _uiState.value.copy(selectedCampus = campus)
    }
}