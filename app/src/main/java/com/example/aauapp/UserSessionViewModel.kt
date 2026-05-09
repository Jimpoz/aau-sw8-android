package com.example.aauapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserSessionState(
    val user: User? = null,
    val selectedCampusId: String? = null,
    val selectedBuildingId: String? = null,
    val selectedFloorId: String? = null,
    val error: String? = null,
) {
    val isLoggedIn: Boolean
        get() = profile.id.isNotBlank()
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

    init {
        viewModelScope.launch {
            store.profileFlow.collect { profile ->
                AuthTokenStore.token = profile.authToken
                _uiState.value = UserSessionUiState(
                    isLoading = false,
                    profile = profile
                )
            }
        }
    }

    fun login(email: String, password: String, organizationId: String?) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Email and password are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)

            try {
                val response = repository.login(
                    email = email.trim(),
                    password = password,
                    organizationId = organizationId?.ifBlank { null }
                )

                if (response.mfa_required) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        mfaChallengeToken = response.challenge_token,
                        error = null
                    )
                    return@launch
                }

                saveAuthResponse(response)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Login failed"
                )
            }
        }
    }

    fun completeMfaLogin(code: String) {
        val challenge = _uiState.value.mfaChallengeToken

        if (challenge.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(error = "Missing MFA challenge")
            return
        }

        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter the verification code")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)

            try {
                val response = repository.loginMfa(
                    challengeToken = challenge,
                    code = code.trim()
                )

                saveAuthResponse(response)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Verification failed"
                )
            }
        }
    }

    fun signup(
        email: String,
        password: String,
        fullName: String?,
        organizationId: String?
    ) {
        if (email.isBlank() || password.length < 8) {
            _uiState.value = _uiState.value.copy(
                error = "Email and password are required",
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)

            try {
                saveAuthResponse(
                    repository.signup(
                        email = email.trim(),
                        password = password,
                        fullName = fullName?.ifBlank { null },
                        organizationId = organizationId?.ifBlank { null }
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Signup failed"
                )
            }
        }
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

    private suspend fun saveAuthResponse(response: AuthResponseDto) {
        val token = response.token

        if (token.isNullOrBlank()) {
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

        store.saveProfile(profile)

        _uiState.value = UserSessionUiState(
            isLoading = false,
            profile = profile,
            error = null,
            mfaChallengeToken = null,
            message = null
        )
    }

    fun enableEmailMfa(
        onChallengeReady: (String, List<String>) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)

            try {
                val result = repository.setupMfaEmail()

                _uiState.value = _uiState.value.copy(isLoading = false)

                onChallengeReady(
                    result.setup_challenge_token,
                    result.recovery_codes
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Could not start two-factor setup"
                )
            }
        }
    }

    fun confirmEmailMfa(
        challengeToken: String,
        code: String
    ) {
        if (challengeToken.isBlank() || code.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter the verification code")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)

            try {
                val result = repository.confirmMfaEmail(
                    challengeToken = challengeToken,
                    code = code.trim()
                )

                val profile = _uiState.value.profile.copy(
                    mfaEnabled = result.mfa_enabled,
                    mfaMethod = result.mfa_method
                )

                store.saveProfile(profile)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = profile,
                    message = "Two-factor authentication enabled"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Could not confirm two-factor authentication"
                )
            }
        }
    }

    fun disableMfa(password: String) {
        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter your password")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)

            try {
                val result = repository.disableMfa(password)

                val profile = _uiState.value.profile.copy(
                    mfaEnabled = result.mfa_enabled,
                    mfaMethod = result.mfa_method
                )

                store.saveProfile(profile)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = profile,
                    message = "Two-factor authentication disabled"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Could not disable two-factor authentication"
                )
            }
        }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String
    ) {
        if (currentPassword.isBlank() || newPassword.length < 8) {
            _uiState.value = _uiState.value.copy(
                error = "Current password and 8-character new password are required"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)

            try {
                repository.changePassword(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Password changed"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Could not change password"
                )
            }
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter your email")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)

            try {
                repository.forgotPassword(email.trim())

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Recovery code sent"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Could not send recovery code"
                )
            }
        }
    }

    fun resetPassword(
        email: String,
        code: String,
        newPassword: String
    ) {
        if (email.isBlank() || code.isBlank() || newPassword.length < 8) {
            _uiState.value = _uiState.value.copy(
                error = "Email, code and 8-character password are required"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)

            try {
                repository.resetPassword(
                    email = email.trim(),
                    code = code.trim(),
                    newPassword = newPassword
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Password reset complete"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Could not reset password"
                )
            }
        }
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
