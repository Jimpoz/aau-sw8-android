package com.example.aauapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aauapp.ui.theme.AndroidCard
import com.example.aauapp.ui.theme.Blue600
import com.example.aauapp.ui.theme.Slate50
import com.example.aauapp.ui.theme.Slate500

@Composable
fun MainScreen(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    userSessionViewModel: UserSessionViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showRoomUpload by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Slate50,
        bottomBar = {
            if (!showRoomUpload) {
                NavigationBar(
                    containerColor = AndroidCard,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Map, contentDescription = null) },
                        label = { Text("Map") },
                        colors = navColors()
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null) },
                        label = { Text("Assistant") },
                        colors = navColors()
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Outlined.CameraAlt, contentDescription = null) },
                        label = { Text("Camera") },
                        colors = navColors()
                    )

                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                        label = { Text("Explore") },
                        colors = navColors()
                    )

                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Profile") },
                        colors = navColors()
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .background(Slate50)
                .padding(paddingValues)
        ) {
            if (showRoomUpload) {
                RoomPhotoUploadScreen(onBack = { showRoomUpload = false })
            } else {
                when (selectedTab) {
                    0 -> FloorPlanScreen(floorId = "floor-1")
                    1 -> AssistantScreen()
                    2 -> CameraScreen(
                        onScanRoom = { showRoomUpload = true }
                    )
                    3 -> ExploreScreen()
                    4 -> ProfileScreen(
                        isDarkMode = isDarkMode,
                        onDarkModeChange = onDarkModeChange,
                        onOpenRoomPhotoUpload = { showRoomUpload = true },
                        viewModel = userSessionViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Blue600,
    selectedTextColor = Blue600,
    unselectedIconColor = Slate500,
    unselectedTextColor = Slate500,
    indicatorColor = AndroidCard
)