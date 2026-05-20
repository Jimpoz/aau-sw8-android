package com.example.aauapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aauapp.data.remote.AuthRepository
import com.example.aauapp.data.remote.AuthResponseDto
import com.example.aauapp.data.remote.AuthTokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserSessionUiState(
    val isLoading: Boolean = true,
    val profile: UserProfileUi = UserProfileUi(),
    val error: String? = null,
    val mfaChallengeToken: String? = null,
    val message: String? = null
) {
    val isLoggedIn: Boolean
        get() = profile.id.isNotBlank()
}

class UserSessionViewModel(
    private val store: UserSessionStore
) : ViewModel() {

    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow(UserSessionUiState())
    val uiState: StateFlow<UserSessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            store.profileFlow.collect { profile ->
                AuthTokenStore.token = profile.authToken
                NavigationPreferenceMemory.wheelchairOnly = profile.wheelchairOnly

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
                    organizationId = organizationId
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
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
                    error = when {
                        e.message?.contains("401") == true ->
                            "Invalid email or password"
                        e.message?.contains("404") == true ->
                            "Organization not found. Leave Organization ID empty or use the correct one."
                        else ->
                            e.message ?: "Login failed"
                    }
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
                error = "Email and 8-character password are required"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)

            try {
                val response = repository.signup(
                    email = email.trim(),
                    password = password,
                    fullName = fullName?.trim()?.takeIf { it.isNotBlank() },
                    organizationId = organizationId
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                )

                saveAuthResponse(response)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = when {
                        e.message?.contains("404") == true ->
                            "Organization not found. Leave Organization ID empty or use the correct one."
                        e.message?.contains("409") == true ->
                            "Account already exists. Try signing in."
                        else ->
                            e.message ?: "Signup failed"
                    }
                )
            }
        }
    }

    fun guestLogin() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)

            try {
                saveAuthResponse(repository.guest())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Guest login failed"
                )
            }
        }
    }

    private suspend fun saveAuthResponse(response: AuthResponseDto) {
        val token = response.token

        if (token.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Auth failed: no token returned"
            )
            return
        }

        AuthTokenStore.token = token

        val user = response.user

        val previous = _uiState.value.profile

        val profile = previous.copy(
            id = user?.id.orEmpty(),
            email = user?.email.orEmpty(),
            displayName = user?.full_name ?: user?.email?.substringBefore("@").orEmpty(),
            membershipTier = response.role ?: "Member",
            organizationId = response.organization_id,
            role = response.role,
            authToken = token
        )

        NavigationPreferenceMemory.wheelchairOnly = profile.wheelchairOnly

        store.saveProfile(profile)

        _uiState.value = UserSessionUiState(
            isLoading = false,
            profile = profile,
            error = null,
            mfaChallengeToken = null,
            message = null
        )
    }

    fun enableMfa(
        onSetupReady: (String, String, List<String>) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = repository.setupMfa()

                _uiState.value = _uiState.value.copy(isLoading = false)

                onSetupReady(
                    result.secret,
                    result.provisioning_uri,
                    result.recovery_codes
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Could not start MFA setup"
                )
            }
        }
    }

    fun confirmMfa(code: String) {
        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter the verification code")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = repository.confirmMfa(code.trim())

                val profile = _uiState.value.profile.copy(
                    mfaEnabled = result.mfa_enabled,
                    mfaMethod = result.mfa_method
                )

                persist(profile, message = "Two-factor authentication enabled")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Could not confirm MFA"
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

                persist(profile, message = "Two-factor authentication disabled")
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
        persist(
            _uiState.value.profile.copy(
                campusId = campusId,
                buildingId = null,
                defaultFloorId = null
            )
        )
    }

    fun updateBuilding(buildingId: String) {
        persist(
            _uiState.value.profile.copy(
                buildingId = buildingId,
                defaultFloorId = null
            )
        )
    }

    fun updateDefaultFloor(floorId: String) {
        persist(
            _uiState.value.profile.copy(
                defaultFloorId = floorId
            )
        )
    }

    fun updateNavigationPreferences(
        avoidStairs: Boolean,
        voiceEnabled: Boolean,
        elevatorsOnly: Boolean,
        wheelchairOnly: Boolean
    ) {
        val profile = _uiState.value.profile.copy(
            avoidStairs = avoidStairs,
            voiceEnabled = voiceEnabled,
            elevatorsOnly = elevatorsOnly,
            wheelchairOnly = wheelchairOnly
        )

        NavigationPreferenceMemory.wheelchairOnly = wheelchairOnly

        persist(profile)
    }

    fun deleteProfile(password: String) {
        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter your password")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                message = null
            )

            try {
                repository.deleteAccount(password)

                AuthTokenStore.token = null
                NavigationPreferenceMemory.wheelchairOnly = false
                store.clearProfile()

                _uiState.value = UserSessionUiState(
                    isLoading = false,
                    message = "Profile deleted"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Could not delete profile"
                )
            }
        }
    }

    fun logout() {
        AuthTokenStore.token = null
        NavigationPreferenceMemory.wheelchairOnly = false

        viewModelScope.launch {
            store.clearProfile()
        }

        _uiState.value = UserSessionUiState(isLoading = false)
    }

    private fun persist(
        profile: UserProfileUi,
        message: String? = null
    ) {
        NavigationPreferenceMemory.wheelchairOnly = profile.wheelchairOnly

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            profile = profile,
            error = null,
            message = message
        )

        viewModelScope.launch {
            store.saveProfile(profile)
        }
    }
}

class UserSessionViewModelFactory(
    private val store: UserSessionStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UserSessionViewModel(store) as T
    }
}