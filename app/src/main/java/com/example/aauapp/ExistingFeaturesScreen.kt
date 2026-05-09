package com.example.aauapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun NavigationToolsScreen(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    userSessionViewModel: UserSessionViewModel,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showFloorPicker by remember { mutableStateOf(true) }

    val sessionState by userSessionViewModel.uiState.collectAsState()
    val profile = sessionState.profile

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Assistant") }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Outlined.Camera, contentDescription = null) },
                    label = { Text("Camera") }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        showFloorPicker = true
                    },
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text("Floor") }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Profile") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> AssistantScreen()

                1 -> CameraScreen()

                2 -> {
                    when {
                        profile.campusId.isNullOrBlank() -> {
                            CampusSelectionScreenWithOpen(
                                userSessionViewModel = userSessionViewModel,
                                onOpenCampus = {
                                    showFloorPicker = true
                                }
                            )
                        }

                        showFloorPicker || profile.defaultFloorId.isNullOrBlank() -> {
                            CampusFloorPickerScreen(
                                campusId = profile.campusId,
                                userSessionViewModel = userSessionViewModel,
                                onOpenFloor = {
                                    showFloorPicker = false
                                }
                            )
                        }

                        else -> {
                            Column {
                                Button(
                                    onClick = { showFloorPicker = true }
                                ) {
                                    Text("Change floor")
                                }

                                FloorPlanScreen(
                                    floorId = profile.defaultFloorId
                                )
                            }
                        }
                    }
                }

                3 -> ProfileScreen(
                    isDarkMode = isDarkMode,
                    onDarkModeChange = onDarkModeChange,
                    onOpenRoomPhotoUpload = {},
                    viewModel = userSessionViewModel
                )
            }
        }
    }
}