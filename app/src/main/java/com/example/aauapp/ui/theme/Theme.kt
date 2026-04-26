package com.example.aauapp.ui.theme

import androidx.compose.material3.MaterialTheme
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

@Composable
fun AAUAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AndroidLightColors,
        typography = Typography,
        content = content
    )
}