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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aauapp.data.remote.BuildingMapDto
import com.example.aauapp.data.remote.CampusDto
import com.example.aauapp.data.remote.FloorMapDto
import com.example.aauapp.ui.theme.*

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Explore",
                style = MaterialTheme.typography.headlineLarge,
                color = Slate900
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Browse campuses, buildings and rooms.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate500
            )
        }

        if (uiState.isLoading) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = AndroidCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Loading backend data...")
                    }
                }
            }
        }

        uiState.error?.let { error ->
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        item {
            SummaryGrid(
                campusCount = uiState.campuses.size,
                buildingCount = uiState.buildings.size,
                floorCount = uiState.floors.size
            )
        }

        item { SectionTitle("Campuses") }

        items(uiState.campuses) { campus ->
            CampusExploreCard(
                campus = campus,
                isSelected = uiState.selectedCampus?.id == campus.id,
                onClick = { viewModel.selectCampus(campus) }
            )
        }

        item { SectionTitle("Buildings") }

        if (uiState.buildings.isEmpty() && !uiState.isLoading) {
            item { EmptyCard("No buildings found for this campus.") }
        } else {
            items(uiState.buildings) { building ->
                BuildingExploreCard(
                    building = building,
                    isSelected = uiState.selectedBuilding?.id == building.id,
                    onClick = { viewModel.selectBuilding(building) }
                )
            }
        }

        item { SectionTitle("Floors") }

        if (uiState.floors.isEmpty() && !uiState.isLoading) {
            item { EmptyCard("Select a building to see floors.") }
        } else {
            items(uiState.floors) { floor ->
                FloorExploreCard(floor = floor)
            }
        }
    }
}

@Composable
private fun SummaryGrid(
    campusCount: Int,
    buildingCount: Int,
    floorCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard("Campuses", campusCount.toString(), Icons.Default.Public, Modifier.weight(1f))
            SummaryCard("Buildings", buildingCount.toString(), Icons.Default.Business, Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard("Floors", floorCount.toString(), Icons.Default.Stairs, Modifier.weight(1f))
            SummaryCard("Rooms", "Backend", Icons.Default.MeetingRoom, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AndroidCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            IconBubble(icon)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = Slate900
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
            )
        }
    }
}

@Composable
private fun CampusExploreCard(
    campus: CampusDto,
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
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(Icons.Default.Map)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = campus.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate900
                )

                Text(
                    text = campus.organization_name
                        ?: campus.organization_id
                        ?: campus.description
                        ?: campus.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }

            if (isSelected) {
                Text(
                    text = "Selected",
                    style = MaterialTheme.typography.labelMedium,
                    color = Blue600
                )
            }
        }
    }
}

@Composable
private fun BuildingExploreCard(
    building: BuildingMapDto,
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
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(Icons.Default.Business)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = building.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate900
                )

                Text(
                    text = listOfNotNull(
                        building.short_name,
                        "${building.floors.size} floors",
                        building.address
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = Slate400
            )
        }
    }
}

@Composable
private fun FloorExploreCard(floor: FloorMapDto) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AndroidCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(Icons.Default.Stairs)

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = floor.display_name ?: floor.id,
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate900
                )

                Text(
                    text = "Index ${floor.floor_index ?: 0} • ${floor.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
        }
    }
}

@Composable
private fun IconBubble(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Blue50),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Blue600,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Slate500,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun EmptyCard(text: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AndroidCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Slate500,
            modifier = Modifier.padding(16.dp)
        )
    }
}