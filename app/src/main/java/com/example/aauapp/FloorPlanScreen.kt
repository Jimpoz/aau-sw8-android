package com.example.aauapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun FloorPlanScreen() {
    val viewModel: FloorPlanViewModel = viewModel()

    val availableFloors by viewModel.availableFloors.collectAsState()
    val selectedFloor by viewModel.selectedFloor.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val selectedRoom by viewModel.selectedRoom.collectAsState()
    val userPosition by viewModel.userPosition.collectAsState()
    val route by viewModel.route.collectAsState()
    val statusText by viewModel.statusText.collectAsState()

    val filteredRooms = if (searchText.isBlank()) {
        rooms
    } else {
        rooms.filter { it.name.contains(searchText, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 170.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFF6F4EE)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawFloorBase()

                    filteredRooms.forEach { room ->
                        drawCircle(
                            color = if (selectedRoom?.id == room.id) Color(0xFFD81B60) else Color(0xFFD32F2F),
                            radius = 20f,
                            center = room.position
                        )
                    }

                    if (route.size > 1) {
                        route.zipWithNext().forEach { (start, end) ->
                            drawLine(
                                color = Color(0xFF1565C0),
                                start = start,
                                end = end,
                                strokeWidth = 10f
                            )
                        }
                    }

                    drawCircle(
                        color = Color(0xFF1E88E5),
                        radius = 22f,
                        center = userPosition
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                SearchBarCard(
                    value = searchText,
                    onValueChange = viewModel::updateSearchText
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableFloors) { floor ->
                        FilterChip(
                            selected = floor.id == selectedFloor,
                            onClick = { viewModel.selectFloor(floor.id) },
                            label = { Text(floor.label) },
                            colors = FilterChipDefaults.filterChipColors()
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledIconButton(onClick = { viewModel.zoomOut() }) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Zoom out")
                }
                FilledIconButton(onClick = { viewModel.zoomIn() }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Zoom in")
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 90.dp),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        BottomRouteCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            title = selectedRoom?.name ?: "Select a destination",
            subtitle = if (selectedRoom != null) "Route preview ready" else "Choose a room to start navigation",
            chips = filteredRooms.take(5),
            onChipClick = viewModel::selectRoom,
            onClear = viewModel::clearSelection
        )
    }
}

@Composable
private fun SearchBarCard(
    value: String,
    onValueChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            placeholder = {
                Text("Search rooms or places")
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        )
    }
}

@Composable
private fun BottomRouteCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    chips: List<RoomUi>,
    onChipClick: (RoomUi) -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(100.dp)
                    )
                    .width(42.dp)
                    .height(5.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledIconButton(onClick = {}) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Start")
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chips) { room ->
                    FilterChip(
                        selected = false,
                        onClick = { onChipClick(room) },
                        label = { Text(room.name) }
                    )
                }
            }

            TextButton(
                onClick = onClear,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Clear")
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFloorBase() {
    drawRect(
        color = Color(0xFFEDE7D9)
    )

    drawRect(
        color = Color(0xFFD6D0C2),
        topLeft = Offset(100f, 160f),
        size = androidx.compose.ui.geometry.Size(760f, 90f)
    )

    drawRect(
        color = Color(0xFFD6D0C2),
        topLeft = Offset(100f, 450f),
        size = androidx.compose.ui.geometry.Size(500f, 90f)
    )

    drawRect(
        color = Color(0xFFD6D0C2),
        topLeft = Offset(100f, 700f),
        size = androidx.compose.ui.geometry.Size(650f, 90f)
    )

    drawRect(
        color = Color(0xFFD6D0C2),
        topLeft = Offset(680f, 250f),
        size = androidx.compose.ui.geometry.Size(90f, 450f)
    )
}