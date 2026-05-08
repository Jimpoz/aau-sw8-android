package com.example.aauapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AndroidLightColors = lightColorScheme(
    primary = Blue600,
    secondary = Slate800,
    background = Slate50,
    surface = AndroidCard,
    surfaceVariant = Slate100,
    primaryContainer = Blue100,
    secondaryContainer = Slate100,
    onPrimary = AndroidCard,
    onSecondary = AndroidCard,
    onBackground = Slate900,
    onSurface = Slate900,
    onSurfaceVariant = Slate500,
    onPrimaryContainer = Blue700,
    onSecondaryContainer = Slate700
)

private val AndroidDarkColors = darkColorScheme(
    primary = Blue400,
    secondary = Slate200,
    background = Slate900,
    surface = Slate800,
    surfaceVariant = Slate700,
    primaryContainer = Blue700,
    secondaryContainer = Slate700,
    onPrimary = AndroidCard,
    onSecondary = Slate900,
    onBackground = AndroidCard,
    onSurface = AndroidCard,
    onSurfaceVariant = Slate300,
    onPrimaryContainer = AndroidCard,
    onSecondaryContainer = AndroidCard
)

@Composable
fun AAUAppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) AndroidDarkColors else AndroidLightColors,
        typography = Typography,
        content = content
    )
}