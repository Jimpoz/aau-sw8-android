package com.example.aauapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aauapp.data.remote.BuildingMapDto
import com.example.aauapp.data.remote.FloorMapDto
import com.example.aauapp.ui.theme.*

@Composable
fun CampusFloorPickerScreen(
    campusId: String,
    onOpenFloor: (String) -> Unit,
    viewModel: CampusFloorPickerViewModel = viewModel(),
    userSessionViewModel: UserSessionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(campusId) {
        viewModel.loadCampus(campusId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = uiState.campusName ?: "Campus",
            style = MaterialTheme.typography.headlineLarge,
            color = Slate900
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Choose a building and floor to open the indoor map.",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate500
        )

        Spacer(modifier = Modifier.height(18.dp))

        when {
            uiState.isLoading -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.error != null -> {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    item {
                        SectionTitle("Buildings")
                    }

                    items(uiState.buildings) { building ->
                        BuildingCard(
                            building = building,
                            isSelected = uiState.selectedBuildingId == building.id,
                            onClick = {
                                viewModel.selectBuilding(building.id)
                                userSessionViewModel.updateBuilding(building.id)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        SectionTitle("Floors")
                    }

                    items(uiState.floors) { floor ->
                        FloorCard(
                            floor = floor,
                            isSelected = uiState.selectedFloorId == floor.id,
                            onClick = {
                                viewModel.selectFloor(floor.id)
                            }
                        )
                    }
                }

                uiState.selectedFloorId?.let { selectedFloorId ->
                    Button(
                        onClick = {
                            userSessionViewModel.updateDefaultFloor(selectedFloorId)
                            onOpenFloor(selectedFloorId)
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp)
                            .height(54.dp)
                    ) {
                        Text("Open Floor")
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildingCard(
    building: BuildingMapDto,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    PickerCard(
        title = building.name,
        subtitle = "${building.floors.size} floors • ${building.id}",
        icon = Icons.Default.Business,
        isSelected = isSelected,
        onClick = onClick
    )
}

@Composable
private fun FloorCard(
    floor: FloorMapDto,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    PickerCard(
        title = floor.display_name ?: floor.id,
        subtitle = "Floor index: ${floor.floor_index ?: 0} • ${floor.id}",
        icon = Icons.Default.Layers,
        isSelected = isSelected,
        onClick = onClick
    )
}

@Composable
private fun PickerCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Blue50 else AndroidCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Blue600 else Blue50),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) AndroidCard else Blue600
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate900
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Slate400,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}