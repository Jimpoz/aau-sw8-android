package com.example.aauapp.data.remote

class AuthRepository(
    private val api: BackendApi = ApiModule.backendApi
) {
    suspend fun login(
        email: String,
        password: String,
        organizationId: String?
    ): AuthResponseDto {
        return api.login(
            AuthLoginRequest(
                email = email,
                password = password,
                organization_id = organizationId
            )
        )
    }

    suspend fun signup(
        email: String,
        password: String,
        fullName: String?,
        organizationId: String?
    ): AuthResponseDto {
        return api.signup(
            AuthSignupRequest(
                email = email,
                password = password,
                full_name = fullName,
                organization_id = organizationId
            )
        )
    }

    suspend fun loginMfa(
        challengeToken: String,
        code: String
    ): AuthResponseDto {
        return api.loginMfa(
            AuthMfaLoginRequest(
                challenge_token = challengeToken,
                code = code
            )
        )
    }

    suspend fun guest(): AuthResponseDto {
        return api.guest()
    }

    suspend fun me(): AuthMeDto {
        return api.me()
    }

    suspend fun setupMfaEmail(): MfaEmailSetupResponseDto {
        return api.setupMfaEmail()
    }

    suspend fun confirmMfaEmail(
        challengeToken: String,
        code: String
    ): MfaStateResponseDto {
        return api.confirmMfaEmail(
            MfaEmailConfirmRequestDto(
                challenge_token = challengeToken,
                code = code
            )
        )
    }

    suspend fun disableMfa(password: String): MfaStateResponseDto {
        return api.disableMfa(
            MfaDisableRequestDto(
                password = password
            )
        )
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ) {
        api.changePassword(
            PasswordChangeRequestDto(
                current_password = currentPassword,
                new_password = newPassword
            )
        )
    }

    suspend fun forgotPassword(email: String) {
        api.forgotPassword(
            PasswordForgotRequestDto(
                email = email
            )
        )
    }

    suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String
    ) {
        api.resetPassword(
            PasswordResetRequestDto(
                email = email,
                code = code,
                new_password = newPassword
            )
        )
    }
}