package com.example.aauapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aauapp.ui.theme.AndroidCard
import com.example.aauapp.ui.theme.Blue600
import com.example.aauapp.ui.theme.Slate50
import com.example.aauapp.ui.theme.Slate500

enum class AppDestination(val title: String) {
    MainMenu(""),
    NavigationTools("Navigation Tools"),
    InteractiveMap("Interactive Map"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userSessionViewModel: UserSessionViewModel,
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.MainMenu) }

    BackHandler(enabled = currentDestination != AppDestination.MainMenu) {
        currentDestination = AppDestination.MainMenu
    }

    Scaffold(
        topBar = {
            if (currentDestination != AppDestination.MainMenu) {
                TopAppBar(
                    title = { Text(currentDestination.title) },
                    navigationIcon = {
                        IconButton(onClick = { currentDestination = AppDestination.MainMenu }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to main menu",
                            )
                        }
                    },
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentDestination) {
                AppDestination.MainMenu -> MainMenuScreen(
                    onOpenNavigationTools = {
                        currentDestination = AppDestination.NavigationTools
                    },
                    onOpenInteractiveMap = {
                        currentDestination = AppDestination.InteractiveMap
                    },
                    onLogout = userSessionViewModel::logout,
                )
                AppDestination.NavigationTools -> NavigationToolsScreen(
                    userSessionViewModel = userSessionViewModel,
                )
                AppDestination.InteractiveMap -> InteractiveRoomMapScreen()
            }
        }
    }
}
