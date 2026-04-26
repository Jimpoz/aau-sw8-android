package com.example.aauapp

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSessionDataStore by preferencesDataStore(name = "user_session")

class UserSessionStore(private val context: Context) {

    private object Keys {
        val PROFILE_JSON = stringPreferencesKey("profile_json")
    }

    private val gson = Gson()

    val profileFlow: Flow<UserProfileUi> =
        context.userSessionDataStore.data.map { preferences ->
            val json = preferences[Keys.PROFILE_JSON]
            if (json.isNullOrBlank()) {
                UserProfileUi()
            } else {
                runCatching {
                    gson.fromJson(json, UserProfileUi::class.java)
                }.getOrElse {
                    UserProfileUi()
                }
            }
        }

    suspend fun saveProfile(profile: UserProfileUi) {
        context.userSessionDataStore.edit { preferences ->
            preferences[Keys.PROFILE_JSON] = gson.toJson(profile)
        }
    }

    suspend fun clearProfile() {
        context.userSessionDataStore.edit { preferences ->
            preferences.remove(Keys.PROFILE_JSON)
        }
    }
}