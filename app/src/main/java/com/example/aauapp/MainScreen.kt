package com.example.aauapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aauapp.ui.theme.Blue600

@Composable
fun MainScreen(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    userSessionViewModel: UserSessionViewModel
) {
    val session by userSessionViewModel.uiState.collectAsState()
    val profile = session.profile

    val floorPlanViewModel: FloorPlanViewModel = viewModel()
    val floorState by floorPlanViewModel.uiState.collectAsState()

    var selectedTab by rememberSaveable("main_selected_tab") {
        mutableIntStateOf(0)
    }

    var showRoomUpload by rememberSaveable("show_room_upload") {
        mutableStateOf(false)
    }

    var pendingCameraDetection by rememberSaveable("pending_camera_detection") {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(profile.defaultFloorId, profile.buildingId) {
        val floorId = profile.defaultFloorId
        val buildingId = profile.buildingId
        when {
            !floorId.isNullOrBlank() -> floorPlanViewModel.loadFloor(floorId)
            !buildingId.isNullOrBlank() ->
                floorPlanViewModel.loadDefaultFloorForBuilding(buildingId)
        }
    }

    LaunchedEffect(pendingCameraDetection, floorState.spaces) {
        val detected = pendingCameraDetection

        if (!detected.isNullOrBlank() && floorState.spaces.isNotEmpty()) {
            floorPlanViewModel.selectBestMatchingSpace(detected)
            pendingCameraDetection = null
        }
    }

    BackHandler(enabled = showRoomUpload) {
        showRoomUpload = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!showRoomUpload) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                        },
                        icon = {
                            Icon(Icons.Default.Map, contentDescription = null)
                        },
                        label = {
                            Text("Map")
                        },
                        colors = navColors()
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                        },
                        icon = {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null)
                        },
                        label = {
                            Text("Assistant")
                        },
                        colors = navColors()
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                        },
                        icon = {
                            Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                        },
                        label = {
                            Text("Camera")
                        },
                        colors = navColors()
                    )

                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = {
                            selectedTab = 3
                        },
                        icon = {
                            Icon(Icons.Default.Explore, contentDescription = null)
                        },
                        label = {
                            Text("Explore")
                        },
                        colors = navColors()
                    )

                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = {
                            selectedTab = 4
                        },
                        icon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        label = {
                            Text("Profile")
                        },
                        colors = navColors()
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (showRoomUpload) {
                RoomPhotoUploadScreen(
                    onBack = {
                        showRoomUpload = false
                    }
                )
            } else {
                when (selectedTab) {
                    0 -> {
                        val mapRole = profile.role?.lowercase()
                        GoogleMapScreen(
                            floorId = floorState.floorId ?: profile.defaultFloorId,
                            floorName = floorState.floorName ?: "Ground Floor",
                            canCalibrate = mapRole == "owner" || mapRole == "editor",
                            viewModel = floorPlanViewModel
                        )
                    }

                    1 -> {
                        AssistantScreen(
                            campusId = profile.campusId ?: "campus-aau-cph",
                            buildingId = profile.buildingId
                        )
                    }

                    2 -> {
                        val role = profile.role?.lowercase()
                        CameraScreen(
                            facilityId = floorState.currentCampusId
                                ?: profile.campusId ?: "aau",
                            canRegisterLandmark = role == "owner" || role == "editor",
                            buildingId = floorState.currentBuildingId
                                ?: profile.buildingId,
                            preferredFloorId = floorState.floorId
                                ?: profile.defaultFloorId,
                            onScanRoom = {
                                showRoomUpload = true
                            },
                            onAskDirections = { destination ->
                                pendingCameraDetection = destination
                                selectedTab = 0
                            },
                            onLocationSnap = { location ->
                                val spaceId = location.space_id ?: location.id
                                if (spaceId.isNotBlank()) {
                                    floorPlanViewModel.forceUserSpace(
                                        spaceId = spaceId,
                                        floorId = location.floor_id
                                    )
                                    selectedTab = 0
                                }
                            }
                        )
                    }

                    3 -> {
                        ExploreScreen(
                            onOpenInMap = { floorId, spaceId ->
                                floorPlanViewModel.openInMap(floorId, spaceId)
                                selectedTab = 0
                            }
                        )
                    }

                    4 -> {
                        ProfileScreen(
                            isDarkMode = isDarkMode,
                            onDarkModeChange = onDarkModeChange,
                            onOpenRoomPhotoUpload = {
                                showRoomUpload = true
                            },
                            viewModel = userSessionViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Blue600,
    selectedTextColor = Blue600,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
)