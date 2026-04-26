package com.example.aauapp

import android.content.Context

class AppContainer(context: Context) {
    val themePreferencesStore = ThemePreferencesStore(context)
    val userSessionStore = UserSessionStore(context)
    val locationService = LocationService(context)
}