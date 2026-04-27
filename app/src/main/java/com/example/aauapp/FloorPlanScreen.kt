package com.example.aauapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
        if (floorId.isNotBlank()) {
            viewModel.loadFloor(floorId)
        }
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
                text = uiState.floorName ?: "Floor: $floorId",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate500
            )

            Spacer(modifier = Modifier.height(12.dp))

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

            IOSMapCard(
                spaces = uiState.spaces,
                selectedSpaceId = uiState.selectedSpaceId,
                routeSteps = uiState.routeSteps,
                onSpaceClick = { viewModel.selectSpace(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.computeRouteToSelected() },
                enabled = uiState.selectedSpaceId != null && uiState.spaces.size > 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
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
private fun IOSMapCard(
    spaces: List<SpaceDisplayDto>,
    selectedSpaceId: String?,
    routeSteps: List<com.example.aauapp.data.remote.RouteStepDto>,
    onSpaceClick: (String) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = AndroidCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Blue50)
                    .pointerInput(spaces, scale, offset) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.7f, 4f)
                            offset += pan
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (spaces.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = Blue600,
                            modifier = Modifier.size(56.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "No map spaces found",
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate700
                        )

                        Text(
                            text = "Check floor id or imported backend data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500
                        )
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val allPoints = spaces.flatMap { it.polygon.orEmpty() }

                        val maxX = allPoints.maxOfOrNull { it.getOrNull(0) ?: 0.0 } ?: 1.0
                        val maxY = allPoints.maxOfOrNull { it.getOrNull(1) ?: 0.0 } ?: 1.0

                        val padding = 24f
                        val baseScale = minOf(
                            (size.width - padding * 2) / maxX.toFloat().coerceAtLeast(1f),
                            (size.height - padding * 2) / maxY.toFloat().coerceAtLeast(1f)
                        )

                        fun mapPoint(point: List<Double>): Offset {
                            val x = ((point.getOrNull(0) ?: 0.0).toFloat() * baseScale * scale) + padding + offset.x
                            val y = ((point.getOrNull(1) ?: 0.0).toFloat() * baseScale * scale) + padding + offset.y
                            return Offset(x, y)
                        }

                        spaces.forEach { space ->
                            val polygon = space.polygon ?: return@forEach
                            if (polygon.size < 3) return@forEach

                            val path = Path()

                            polygon.forEachIndexed { index, point ->
                                val mapped = mapPoint(point)
                                if (index == 0) {
                                    path.moveTo(mapped.x, mapped.y)
                                } else {
                                    path.lineTo(mapped.x, mapped.y)
                                }
                            }

                            path.close()

                            val isSelected = selectedSpaceId == space.id
                            val isRoute = routeSteps.any { it.space_id == space.id }

                            drawPath(
                                path = path,
                                color = when {
                                    isSelected -> Blue600.copy(alpha = 0.35f)
                                    isRoute -> Blue600.copy(alpha = 0.22f)
                                    else -> AndroidCard.copy(alpha = 0.95f)
                                }
                            )

                            drawPath(
                                path = path,
                                color = when {
                                    isSelected -> Blue600
                                    isRoute -> Blue600.copy(alpha = 0.8f)
                                    else -> Slate300
                                },
                                style = Stroke(width = if (isSelected) 4f else 2f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Backend Floor Display",
                style = MaterialTheme.typography.headlineMedium,
                color = Slate900
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${spaces.size} spaces loaded",
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = space.display_name ?: space.id,
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate900
                )

                Spacer(modifier = Modifier.height(3.dp))

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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Slate900
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate500
            )
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