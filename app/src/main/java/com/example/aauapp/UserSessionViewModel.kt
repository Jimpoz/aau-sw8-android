package com.example.aauapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSessionState(
    val user: User? = null,
    val selectedCampusId: String? = null,
    val selectedBuildingId: String? = null,
    val selectedFloorId: String? = null,
    val error: String? = null,
) {
    val isLoggedIn: Boolean
        get() = user != null
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val organizationId: String? = null,
    val preferredCampusId: String? = null,
    val preferredBuildingId: String? = null,
    val preferredFloorId: String? = null,
)

class UserSessionViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val sessionStore = UserSessionStore(application.applicationContext)

    private val _uiState = MutableStateFlow(sessionStore.loadSession())
    val uiState: StateFlow<UserSessionState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Email and password are required",
            )
            return
        }

        val name = email.substringBefore("@").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }

        updateSession(
            UserSessionState(
                user = User(
                    id = "local-user-1",
                    name = name,
                    email = email,
                    organizationId = "org-aau",
                ),
                error = null,
            ),
        )
    }

    fun register(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "All fields are required",
            )
            return
        }

        updateSession(
            UserSessionState(
                user = User(
                    id = "local-user-1",
                    name = name,
                    email = email,
                    organizationId = "org-aau",
                ),
                error = null,
            ),
        )
    }

    fun updateCampus(campusId: String) {
        val currentUser = _uiState.value.user

        updateSession(
            _uiState.value.copy(
                selectedCampusId = campusId,
                user = currentUser?.copy(preferredCampusId = campusId),
                error = null,
            ),
        )
    }

    fun updateBuilding(buildingId: String) {
        val currentUser = _uiState.value.user

        updateSession(
            _uiState.value.copy(
                selectedBuildingId = buildingId,
                user = currentUser?.copy(preferredBuildingId = buildingId),
                error = null,
            ),
        )
    }

    fun updateDefaultFloor(floorId: String) {
        val currentUser = _uiState.value.user

        updateSession(
            _uiState.value.copy(
                selectedFloorId = floorId,
                user = currentUser?.copy(preferredFloorId = floorId),
                error = null,
            ),
        )
    }

    fun logout() {
        sessionStore.clearSession()
        _uiState.value = UserSessionState()
    }

    private fun updateSession(newState: UserSessionState) {
        _uiState.value = newState
        sessionStore.saveSession(newState)
    }
}
