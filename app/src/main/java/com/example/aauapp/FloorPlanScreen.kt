package com.example.aauapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Navigation
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
import com.example.aauapp.data.remote.SpaceDisplayDto
import com.example.aauapp.ui.theme.*

@Composable
fun FloorPlanScreen(
    floorId: String,
    viewModel: FloorPlanViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(floorId) {
        viewModel.loadFloor(floorId)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp)
    ) {
        item {
            Text(
                text = "Map",
                style = MaterialTheme.typography.headlineLarge,
                color = Slate900
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = uiState.floorName ?: "Current floor: $floorId",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate500
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
            }

            uiState.error?.let {
                Text(
                    text = "Backend error: $it",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            MapPreviewCard(uiState.spaces.size)

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { viewModel.computeRouteToSelected() },
                enabled = uiState.selectedSpaceId != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Route to Selected Room")
            }

            Spacer(modifier = Modifier.height(18.dp))

            SectionTitle("Rooms from backend")
        }

        items(uiState.filteredSpaces) { space ->
            SpaceCard(
                space = space,
                selected = uiState.selectedSpaceId == space.id,
                onClick = { viewModel.selectSpace(space.id) }
            )
        }

        if (uiState.routeSteps.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(18.dp))
                SectionTitle("Route")
            }

            items(uiState.routeSteps) { step ->
                RouteStepCard(
                    title = step.display_name ?: step.space_id,
                    subtitle = step.instruction ?: step.space_type.orEmpty()
                )
            }
        }
    }
}

@Composable
private fun MapPreviewCard(spaceCount: Int) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = AndroidCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Blue50, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = Blue600,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Backend Floor Display",
                style = MaterialTheme.typography.headlineMedium,
                color = Slate900
            )

            Text(
                text = "$spaceCount spaces loaded",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate500
            )
        }
    }
}

@Composable
private fun SpaceCard(
    space: SpaceDisplayDto,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Blue50 else AndroidCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (selected) Blue600 else Blue50),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MeetingRoom,
                    contentDescription = null,
                    tint = if (selected) AndroidCard else Blue600
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = space.display_name ?: space.id,
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate900
                )

                Text(
                    text = space.space_type ?: "Space",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )
            }
        }
    }
}

@Composable
private fun RouteStepCard(
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AndroidCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Slate500)
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