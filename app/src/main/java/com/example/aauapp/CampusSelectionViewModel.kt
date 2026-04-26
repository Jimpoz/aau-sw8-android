package com.example.aauapp

import android.util.Log
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
                Log.d("CAMPUS_DEBUG", "Loading campuses...")
                val campuses = repository.getCampuses()
                Log.d("CAMPUS_DEBUG", "Loaded campuses count = ${campuses.size}")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    campuses = campuses,
                    selectedCampus = campuses.firstOrNull(),
                    error = null
                )
            } catch (e: Exception) {
                Log.e("CAMPUS_DEBUG", "Campus load failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.toString()
                )
            }
        }
    }

    fun selectCampus(campus: CampusDto) {
        _uiState.value = _uiState.value.copy(selectedCampus = campus)
    }
}