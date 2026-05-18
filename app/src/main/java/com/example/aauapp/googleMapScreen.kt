package com.example.aauapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aauapp.data.remote.RouteStepDto
import com.example.aauapp.data.remote.SpaceDisplayDto
import com.example.aauapp.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun googleMapScreen(
    floorId: String,
    floorName: String = "Ground Floor",
    onChangeFloor: () -> Unit,
    onEditIndoorMap: () -> Unit,
    viewModel: FloorPlanViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(floorId) {
        if (floorId.isNotBlank()) {
            viewModel.loadFloor(floorId)
        }
    }

    val aauCph = LatLng(55.65075, 12.54005)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(aauCph, 18f)
    }

    val suggestions = remember(searchText, uiState.spaces) {
        val q = searchText.trim()
        if (q.isBlank()) {
            emptyList()
        } else {
            uiState.spaces.filter {
                it.display_name.orEmpty().contains(q, ignoreCase = true) ||
                        it.short_name.orEmpty().contains(q, ignoreCase = true) ||
                        it.id.contains(q, ignoreCase = true)
            }.take(5)
        }
    }

    val selectedSpace = uiState.spaces.firstOrNull {
        it.id == uiState.selectedSpaceId
    }

    Box(modifier = Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = false),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false
            )
        )

        LocalFloorOverlay(
            spaces = uiState.spaces,
            selectedSpaceId = uiState.selectedSpaceId,
            routeSteps = uiState.routeSteps,
            onSpaceClick = { viewModel.selectSpace(it) },
            modifier = Modifier.fillMaxSize()
        )

        if (uiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }

        SearchOverlay(
            searchText = searchText,
            onSearchTextChange = { searchText = it },
            suggestions = suggestions,
            onSuggestionClick = { space ->
                searchText = ""
                viewModel.selectSpace(space.id)
            },
            onNavigateClick = {
                viewModel.computeRouteToSelected()
            }
        )

        LocationPill(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 126.dp, start = 16.dp)
        )

        RightControls(
            floorName = floorName,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 118.dp, end = 16.dp),
            onRefresh = { viewModel.loadFloor(floorId) },
            onZoomIn = {
                scope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.zoomIn(), 250)
                }
            },
            onZoomOut = {
                scope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.zoomOut(), 250)
                }
            },
            onChangeFloor = onChangeFloor
        )

        Button(
            onClick = onEditIndoorMap,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 150.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9500),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Tune, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit")
        }

        IconButton(
            onClick = {
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(aauCph, 18f),
                        600
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 142.dp)
                .size(74.dp)
                .clip(CircleShape)
                .background(Color(0xFF007AFF))
        ) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        BottomRouteCard(
            selectedSpace = selectedSpace,
            routeSteps = uiState.routeSteps,
            hasRoute = uiState.routeSteps.isNotEmpty(),
            onClose = { searchText = "" },
            onNavigate = { viewModel.computeRouteToSelected() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
        )

        uiState.error?.let {
            ErrorPill(
                text = it,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(20.dp)
            )
        }
    }
}

@Composable
private fun LocalFloorOverlay(
    spaces: List<SpaceDisplayDto>,
    selectedSpaceId: String?,
    routeSteps: List<RouteStepDto>,
    onSpaceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val drawableSpaces = spaces.filter {
        it.polygon.orEmpty().size >= 3
    }

    if (drawableSpaces.isEmpty()) return

    Canvas(
        modifier = modifier.padding(
            top = 125.dp,
            bottom = 155.dp,
            start = 20.dp,
            end = 20.dp
        )
    ) {
        val allPoints = drawableSpaces.flatMap { it.polygon.orEmpty() }

        val minX = allPoints.minOf { it.getOrNull(0) ?: 0.0 }
        val maxX = allPoints.maxOf { it.getOrNull(0) ?: 0.0 }
        val minY = allPoints.minOf { it.getOrNull(1) ?: 0.0 }
        val maxY = allPoints.maxOf { it.getOrNull(1) ?: 0.0 }

        val widthRange = (maxX - minX).coerceAtLeast(1.0)
        val heightRange = (maxY - minY).coerceAtLeast(1.0)

        val scale = minOf(
            size.width / widthRange.toFloat(),
            size.height / heightRange.toFloat()
        ) * 0.9f

        val offsetX = (size.width - widthRange.toFloat() * scale) / 2f
        val offsetY = (size.height - heightRange.toFloat() * scale) / 2f

        fun toScreen(point: List<Double>): Offset {
            val x = point.getOrNull(0) ?: 0.0
            val y = point.getOrNull(1) ?: 0.0

            return Offset(
                x = offsetX + ((x - minX).toFloat() * scale),
                y = offsetY + ((y - minY).toFloat() * scale)
            )
        }

        drawableSpaces.forEach { space ->
            val path = Path()

            space.polygon.orEmpty().forEachIndexed { index, point ->
                val screenPoint = toScreen(point)

                if (index == 0) {
                    path.moveTo(screenPoint.x, screenPoint.y)
                } else {
                    path.lineTo(screenPoint.x, screenPoint.y)
                }
            }

            path.close()

            val fillColor = if (space.id == selectedSpaceId) {
                Color(0xEE009DFF)
            } else {
                floorFillColor(space)
            }

            drawPath(
                path = path,
                color = fillColor
            )

            drawPath(
                path = path,
                color = Color(0xFF1F2937),
                style = Stroke(
                    width = if (space.id == selectedSpaceId) 5f else 3f
                )
            )
        }

        val routeSpaces = routeSteps.mapNotNull { step ->
            drawableSpaces.firstOrNull { it.id == step.space_id }
        }

        if (routeSpaces.size >= 2) {
            val routePath = Path()

            routeSpaces.forEachIndexed { index, space ->
                val polygon = space.polygon.orEmpty()
                val centerX = polygon.mapNotNull { it.getOrNull(0) }.average()
                val centerY = polygon.mapNotNull { it.getOrNull(1) }.average()
                val point = toScreen(listOf(centerX, centerY))

                if (index == 0) {
                    routePath.moveTo(point.x, point.y)
                } else {
                    routePath.lineTo(point.x, point.y)
                }
            }

            drawPath(
                path = routePath,
                color = Color.White,
                style = Stroke(width = 18f)
            )

            drawPath(
                path = routePath,
                color = Color(0xFF0A84FF),
                style = Stroke(width = 10f)
            )
        }
    }
}

@Composable
private fun SearchOverlay(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    suggestions: List<SpaceDisplayDto>,
    onSuggestionClick: (SpaceDisplayDto) -> Unit,
    onNavigateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 38.dp, start = 16.dp, end = 16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AndroidCard.copy(alpha = 0.96f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 14.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Slate500,
                    modifier = Modifier.size(30.dp)
                )

                TextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    placeholder = {
                        Text(
                            "Search destinations...",
                            color = Slate400,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Blue600
                    )
                )

                IconButton(
                    onClick = onNavigateClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFEAF3FF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AndroidCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    suggestions.forEach { space ->
                        TextButton(
                            onClick = { onSuggestionClick(space) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = space.display_name ?: space.short_name ?: space.id,
                                    color = Slate900
                                )

                                Text(
                                    text = space.space_type ?: "Space",
                                    color = Slate500,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationPill(modifier: Modifier = Modifier) {
    Button(
        onClick = {},
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AndroidCard,
            contentColor = Slate900
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = null,
            tint = Color(0xFF007AFF),
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(7.dp))

        Text("GPS", style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun RightControls(
    floorName: String,
    modifier: Modifier,
    onRefresh: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onChangeFloor: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        WhiteFloatingIcon(
            icon = Icons.Default.Refresh,
            onClick = onRefresh
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AndroidCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 7.dp)
        ) {
            Column {
                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                }

                HorizontalDivider(color = Color(0xFFE0E0E0))

                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, tint = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onChangeFloor,
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007AFF),
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(4.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(
                text = floorName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun WhiteFloatingIcon(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AndroidCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(58.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color.Black)
        }
    }
}

@Composable
private fun BottomRouteCard(
    selectedSpace: SpaceDisplayDto?,
    routeSteps: List<RouteStepDto>,
    hasRoute: Boolean,
    onClose: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val destinationTitle = selectedSpace?.display_name
        ?: selectedSpace?.short_name
        ?: "Destination"

    val instruction = routeSteps.firstOrNull()?.instruction
        ?: if (selectedSpace == null) {
            "Search or select a destination"
        } else {
            "Route to $destinationTitle"
        }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0057D9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasRoute) {
                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                    } else {
                        Icons.Default.Navigation
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasRoute) "NOW" else "READY",
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.labelLarge
                )

                Text(
                    text = instruction,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2
                )

                Text(
                    text = "${routeSteps.size.coerceAtLeast(0)} steps left • $destinationTitle",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onNavigate,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun ErrorPill(text: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.92f)
        )
    ) {
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier.padding(14.dp)
        )
    }
}

private fun floorFillColor(space: SpaceDisplayDto): Color {
    val type = space.space_type.orEmpty().uppercase()

    return when {
        "CORRIDOR" in type || "PASSAGE" in type || "LOBBY" in type ->
            Color(0xEE009DFF)

        "RESTROOM" in type || "WC" in type ->
            Color(0xFFFFA640)

        "STAIR" in type || "ELEVATOR" in type ->
            Color(0xFFE7E4EC)

        else ->
            Color(0xCCB6DFFF)
    }
}