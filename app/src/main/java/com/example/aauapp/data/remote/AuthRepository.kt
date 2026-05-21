package com.example.aauapp.data.remote

class AuthRepository(
    private val api: BackendApi = ApiModule.backendApi
) {
    suspend fun login(email: String, password: String, organizationId: String?): AuthResponseDto =
        api.login(AuthLoginRequest(email, password, organizationId))

    suspend fun signup(email: String, password: String, fullName: String?, organizationId: String?): AuthResponseDto =
        api.signup(AuthSignupRequest(email, password, fullName, organizationId))

    suspend fun loginMfa(challengeToken: String, code: String): AuthResponseDto =
        api.loginMfa(AuthMfaLoginRequest(challengeToken, code))

    suspend fun guest(): AuthResponseDto = api.guest()

    suspend fun me(): AuthMeDto = api.me()

    suspend fun setupMfa(): MfaSetupResponseDto {
        return ApiModule.backendApi.setupMfa()
    }

    suspend fun confirmMfa(code: String): MfaStateResponseDto {
        return ApiModule.backendApi.confirmMfa(
            MfaConfirmRequestDto(code = code)
        )
    }

    suspend fun disableMfa(password: String): MfaStateResponseDto {
        return ApiModule.backendApi.disableMfa(
            MfaDisableRequestDto(password = password)
        )
    }

    suspend fun setupMfaEmail(): MfaEmailSetupResponseDto = api.setupMfaEmail()

    suspend fun confirmMfaEmail(challengeToken: String, code: String): MfaStateResponseDto =
        api.confirmMfaEmail(MfaEmailConfirmRequestDto(challengeToken, code))
    suspend fun changePassword(currentPassword: String, newPassword: String) {
        api.changePassword(PasswordChangeRequestDto(currentPassword, newPassword))
    }

    suspend fun forgotPassword(email: String) {
        api.forgotPassword(PasswordForgotRequestDto(email))
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String) {
        api.resetPassword(PasswordResetRequestDto(email, code, newPassword))
    }

    suspend fun deleteAccount(password: String) {
        val response = api.deleteAccount(DeleteAccountRequestDto(password))

        if (!response.isSuccessful) {
            throw Exception("Could not delete profile")
        }
    }
}