package com.example.aauapp

import android.content.Context
import com.google.gson.Gson

private data class PersistedUserSession(
    val user: User? = null,
    val selectedCampusId: String? = null,
    val selectedBuildingId: String? = null,
    val selectedFloorId: String? = null,
)

class UserSessionStore(
    context: Context,
    private val gson: Gson = Gson(),
) {
    private val sharedPreferences = context.getSharedPreferences(
        "user_session",
        Context.MODE_PRIVATE,
    )

    fun loadSession(): UserSessionState {
        val json = sharedPreferences.getString("session_json", null)
        if (json.isNullOrBlank()) {
            return UserSessionState()
        }

        val persistedSession = runCatching {
            gson.fromJson(json, PersistedUserSession::class.java)
        }.getOrNull() ?: return UserSessionState()

        return UserSessionState(
            user = persistedSession.user,
            selectedCampusId = persistedSession.selectedCampusId,
            selectedBuildingId = persistedSession.selectedBuildingId,
            selectedFloorId = persistedSession.selectedFloorId,
            error = null,
        )
    }

    fun saveSession(state: UserSessionState) {
        val persistedSession = PersistedUserSession(
            user = state.user,
            selectedCampusId = state.selectedCampusId,
            selectedBuildingId = state.selectedBuildingId,
            selectedFloorId = state.selectedFloorId,
        )

        if (persistedSession.user == null) {
            clearSession()
            return
        }

        sharedPreferences.edit()
            .putString("session_json", gson.toJson(persistedSession))
            .apply()
    }

    fun clearSession() {
        sharedPreferences.edit()
            .remove("session_json")
            .apply()
    }
}
