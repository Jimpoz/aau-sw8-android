package com.example.aauapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aauapp.data.remote.BuildingMapDto
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
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp)
    ) {
        item {
            Text(
                text = "Explore",
                style = MaterialTheme.typography.headlineLarge,
                color = Slate900
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Live campus data from backend.",
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

            SectionTitle("Campuses")
        }

        items(uiState.campuses) { campus ->
            ExploreRowCard(
                title = campus.name,
                subtitle = campus.id,
                icon = Icons.Default.Map,
                onClick = { viewModel.loadCampusMap(campus.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(18.dp))
            SectionTitle("Buildings")
        }

        items(uiState.buildings) { building ->
            BuildingCard(building)
        }
    }
}

@Composable
private fun BuildingCard(building: BuildingMapDto) {
    ExploreRowCard(
        title = building.name,
        subtitle = "${building.floors.size} floors",
        icon = Icons.Default.Business,
        onClick = {}
    )
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

@Composable
private fun ExploreRowCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AndroidCard),
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
                    .background(Blue50),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Blue600
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