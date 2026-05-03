package com.example.aauapp

import android.content.ContentResolver
import android.graphics.Paint
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

private data class PositionedSpace(
    val space: MapSpaceUiModel,
    val polygon: List<Offset>,
    val bounds: Rect,
)

private enum class RoomMapAction(
    val title: String,
    val helperText: String,
    val emptyStateText: String,
) {
    ReadRoomData(
        title = "Read room data",
        helperText = "Tap a room to open its Neo4j-backed room information.",
        emptyStateText = "Choose a room on the map to read its data.",
    ),
    AnalyzeRoomImages(
        title = "Analyze room images",
        helperText = "Tap a room to capture or choose four directional images for analysis.",
        emptyStateText = "Choose a room on the map to send images for analysis.",
    ),
}

@Composable
fun InteractiveRoomMapScreen(
    viewModel: RoomMapViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val selectedFloorData = uiState.floors.firstOrNull { it.id == uiState.selectedFloorId }
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedAction by rememberSaveable { mutableStateOf(RoomMapAction.ReadRoomData) }
    var pendingUploadFeedback by remember { mutableStateOf<String?>(null) }
    var pickerDirection by remember { mutableStateOf<UploadDirection?>(null) }
    var cameraDirection by remember { mutableStateOf<UploadDirection?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(uiState.lastUploadResult) {
        val result = uiState.lastUploadResult ?: return@LaunchedEffect
        pendingUploadFeedback = buildUploadFeedbackMessage(result)
        viewModel.finishSuccessfulRoomAnalysis()
    }

    LaunchedEffect(pendingUploadFeedback) {
        val message = pendingUploadFeedback ?: return@LaunchedEffect
        scrollState.animateScrollTo(0)
        snackbarHostState.showSnackbar(
            message = message,
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        pendingUploadFeedback = null
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val direction = pickerDirection
        if (uri != null && direction != null) {
            viewModel.setUploadImage(direction, uri)
        }
        pickerDirection = null
    }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val direction = cameraDirection
        val uri = pendingCameraUri
        if (success && direction != null && uri != null) {
            viewModel.setUploadImage(direction, uri)
        }
        cameraDirection = null
        pendingCameraUri = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Interactive room map",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Choose what a room tap should do, then select the room on the map.",
                style = MaterialTheme.typography.bodyMedium,
            )

            RoomMapActionSelector(
                selectedAction = selectedAction,
                onActionSelected = { selectedAction = it },
            )

            Text(
                text = selectedAction.helperText,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (uiState.campuses.isEmpty() && uiState.isLoadingCampuses) {
                CircularProgressIndicator()
            }

            if (uiState.errorMessage != null && uiState.campuses.isEmpty()) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = viewModel::refreshRoomMap) {
                    Text("Retry")
                }
            }

            if (uiState.campuses.isNotEmpty()) {
                Text(
                    text = "Campus",
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.campuses, key = { it.id }) { campus ->
                        if (campus.id == uiState.selectedCampusId) {
                            Button(onClick = { viewModel.selectCampus(campus.id) }) {
                                Text(campus.name)
                            }
                        } else {
                            OutlinedButton(onClick = { viewModel.selectCampus(campus.id) }) {
                                Text(campus.name)
                            }
                        }
                    }
                }

                if (uiState.isLoadingMap && uiState.floors.isEmpty()) {
                    CircularProgressIndicator()
                }

                if (uiState.floors.isNotEmpty()) {
                    Text(
                        text = "Floor",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.floors, key = { it.id }) { floorData ->
                        if (floorData.id == uiState.selectedFloorId) {
                            Button(onClick = { viewModel.selectFloor(floorData.id) }) {
                                Text(floorData.label)
                            }
                        } else {
                            OutlinedButton(onClick = { viewModel.selectFloor(floorData.id) }) {
                                Text(floorData.label)
                            }
                        }
                    }
                }

                selectedFloorData?.let { floorData ->
                    RoomMapCanvas(
                        floorData = floorData,
                        selectedSpace = uiState.selectedSpace,
                        onSpaceSelected = viewModel::selectSpace,
                    )

                    Text(
                        text = "Spaces on this floor: ${floorData.spaces.size}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Button(onClick = viewModel::refreshRoomMap) {
                    Text(if (uiState.isLoadingCampuses || uiState.isLoadingMap) "Refreshing..." else "Refresh map")
                }

                RoomDetailsCard(
                    action = selectedAction,
                    space = uiState.selectedSpace,
                    contentResolver = context.contentResolver,
                    uploadImages = uiState.selectedUploadImages,
                    isUploading = uiState.isUploadingRoomImages,
                    uploadErrorMessage = uiState.uploadErrorMessage,
                    onPickDirection = { direction ->
                        pickerDirection = direction
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onCaptureDirection = { direction ->
                        val captureUri = createTempImageUri(context, direction)
                        cameraDirection = direction
                        pendingCameraUri = captureUri
                        takePictureLauncher.launch(captureUri)
                    },
                    onClearDirection = viewModel::clearUploadImage,
                    onUploadImages = viewModel::uploadSelectedSpaceImages,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun RoomMapCanvas(
    floorData: MapFloorUiModel,
    selectedSpace: MapSpaceUiModel?,
    onSpaceSelected: (MapSpaceUiModel) -> Unit,
) {
    var canvasSize by remember(floorData.id) { mutableStateOf(IntSize.Zero) }
    var zoom by remember(floorData.id) { mutableStateOf(1f) }
    var pan by remember(floorData.id) { mutableStateOf(Offset.Zero) }
    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.rgb(35, 46, 56)
        }
    }

    val positionedSpaces = remember(floorData.spaces, canvasSize, zoom, pan) {
        layoutSpaces(
            spaces = floorData.spaces,
            canvasSize = canvasSize,
            zoom = zoom,
            pan = pan,
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium,
            ),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F4ED))
                .onSizeChanged { canvasSize = it }
                .pointerInput(floorData.id) {
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(1f, 6f)
                        pan = pan + panChange
                    }
                }
                .pointerInput(floorData.id, positionedSpaces) {
                    detectTapGestures { tapOffset ->
                        positionedSpaces.lastOrNull { containsPoint(it.polygon, tapOffset) }?.space?.let(onSpaceSelected)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                positionedSpaces.forEach { positionedSpace ->
                    val space = positionedSpace.space
                    val isSelected = selectedSpace?.id == space.id
                    val fillColor = when {
                        isSelected -> Color(0xFFD98F2B)
                        space.spaceType.startsWith("ROOM_") -> Color(0xFFBFD7EA)
                        space.spaceType == "CORRIDOR" -> Color(0xFFCDECCF)
                        space.spaceType == "AUDITORIUM" -> Color(0xFFE3D5CA)
                        else -> Color(0xFFDDE3EA)
                    }
                    val borderColor = when {
                        isSelected -> Color(0xFF7A4A05)
                        space.spaceType == "CORRIDOR" -> Color(0xFF3F7C47)
                        else -> Color(0xFF355C7D)
                    }

                    val path = Path().apply {
                        positionedSpace.polygon.forEachIndexed { index, point ->
                            if (index == 0) {
                                moveTo(point.x, point.y)
                            } else {
                                lineTo(point.x, point.y)
                            }
                        }
                        close()
                    }

                    drawPath(
                        path = path,
                        color = fillColor,
                    )
                    drawPath(
                        path = path,
                        color = borderColor,
                        style = Stroke(width = if (isSelected) 4f else 2f),
                    )

                    if (positionedSpace.bounds.width > 72f && positionedSpace.bounds.height > 24f) {
                        labelPaint.textSize = (positionedSpace.bounds.height * 0.24f).coerceIn(14f, 28f)
                        drawContext.canvas.nativeCanvas.drawText(
                            space.shortName ?: space.displayName,
                            positionedSpace.bounds.left + 8f,
                            positionedSpace.bounds.top + labelPaint.textSize + 4f,
                            labelPaint,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomMapActionSelector(
    selectedAction: RoomMapAction,
    onActionSelected: (RoomMapAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Room tap action",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RoomMapAction.entries.forEach { action ->
                    if (action == selectedAction) {
                        Button(
                            onClick = { onActionSelected(action) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(action.title)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onActionSelected(action) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(action.title)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomDetailsCard(
    action: RoomMapAction,
    space: MapSpaceUiModel?,
    contentResolver: ContentResolver,
    uploadImages: Map<UploadDirection, Uri>,
    isUploading: Boolean,
    uploadErrorMessage: String?,
    onPickDirection: (UploadDirection) -> Unit,
    onCaptureDirection: (UploadDirection) -> Unit,
    onClearDirection: (UploadDirection) -> Unit,
    onUploadImages: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (space == null) {
                Text(
                    text = action.emptyStateText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }

            when (action) {
                RoomMapAction.ReadRoomData -> {
                    Text(
                        text = space.displayName,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "${space.campusName} / ${space.floorLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Type: ${space.spaceType}",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    space.shortName?.let {
                        Text(
                            text = "Short name: $it",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Text(
                        text = "Id: ${space.id}",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Text(
                        text = "Building: ${space.buildingName}",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    space.areaM2?.let {
                        Text(
                            text = "Area: ${formatDecimal(it)} m²",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    space.capacity?.let {
                        Text(
                            text = "Capacity: $it",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    if (space.tags.isNotEmpty()) {
                        Text(
                            text = "Tags: ${space.tags.joinToString()}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    space.isAccessible?.let {
                        Text(
                            text = "Accessible: ${if (it) "yes" else "no"}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    space.isNavigable?.let {
                        Text(
                            text = "Navigable: ${if (it) "yes" else "no"}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    space.isOutdoor?.let {
                        Text(
                            text = "Outdoor: ${if (it) "yes" else "no"}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                RoomMapAction.AnalyzeRoomImages -> {
                    Text(
                        text = "Send room images for analysis",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Selected room: ${space.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "${space.buildingName} / ${space.floorLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    if (!supportsImageUpload(space)) {
                        Text(
                            text = "This selected space cannot be analyzed with room images. Choose a room instead of a corridor, staircase, elevator, or escalator.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        return@Column
                    }

                    Text(
                        text = "This flow needs 4 directional images: North, East, South, and West.",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    UploadDirection.entries.forEach { direction ->
                        val uri = uploadImages[direction]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { onPickDirection(direction) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("${direction.label} gallery")
                            }
                            OutlinedButton(
                                onClick = { onCaptureDirection(direction) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("${direction.label} camera")
                            }
                            if (uri != null) {
                                OutlinedButton(
                                    onClick = { onClearDirection(direction) },
                                ) {
                                    Text("Clear")
                                }
                            }
                        }
                        Text(
                            text = uri?.let { resolveDisplayName(contentResolver, it) }
                                ?: "No ${direction.label.lowercase()} image selected",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Button(
                        onClick = onUploadImages,
                        enabled = !isUploading && UploadDirection.entries.all { uploadImages.containsKey(it) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (isUploading) "Uploading..." else "Analyze Room")
                    }

                    if (uploadErrorMessage != null) {
                        Text(
                            text = uploadErrorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun layoutSpaces(
    spaces: List<MapSpaceUiModel>,
    canvasSize: IntSize,
    zoom: Float,
    pan: Offset,
): List<PositionedSpace> {
    if (spaces.isEmpty() || canvasSize.width == 0 || canvasSize.height == 0) {
        return emptyList()
    }

    val allPoints = spaces.flatMap { it.polygon }
    if (allPoints.isEmpty()) {
        return emptyList()
    }

    val minX = allPoints.minOf { it.x }
    val minY = allPoints.minOf { it.y }
    val maxX = allPoints.maxOf { it.x }
    val maxY = allPoints.maxOf { it.y }

    val contentWidth = (maxX - minX).coerceAtLeast(1f)
    val contentHeight = (maxY - minY).coerceAtLeast(1f)
    val padding = 32f
    val baseScale = minOf(
        (canvasSize.width - (padding * 2f)) / contentWidth,
        (canvasSize.height - (padding * 2f)) / contentHeight,
    ).coerceAtLeast(0.1f)
    val scale = baseScale * zoom
    val offsetX = ((canvasSize.width - (contentWidth * scale)) / 2f) + pan.x - (minX * scale)
    val offsetY = ((canvasSize.height - (contentHeight * scale)) / 2f) + pan.y - (minY * scale)

    return spaces.map { space ->
        val polygon = space.polygon.map { point ->
            Offset(
                x = offsetX + (point.x * scale),
                y = offsetY + (point.y * scale),
            )
        }
        val bounds = Rect(
            left = polygon.minOf { it.x },
            top = polygon.minOf { it.y },
            right = polygon.maxOf { it.x },
            bottom = polygon.maxOf { it.y },
        )
        PositionedSpace(
            space = space,
            polygon = polygon,
            bounds = bounds,
        )
    }
}

private fun formatDecimal(value: Float): String = "%.1f".format(value)

private fun buildUploadFeedbackMessage(result: RoomObjectDetectionSetupResponse): String {
    val topObjects = result.roomObjectCounts.entries
        .sortedByDescending { it.value }
        .take(3)
        .joinToString { "${it.key} x${it.value}" }
    val topText = result.roomTextCounts.entries
        .sortedByDescending { it.value }
        .take(2)
        .joinToString { "${it.key} x${it.value}" }

    return buildString {
        append("Room image analysis saved for ${result.roomName}. ")
        append("${result.storedImageCount} images stored")
        if (result.storedViews.isNotEmpty()) {
            append(" (${result.storedViews.joinToString()})")
        }
        if (topObjects.isNotBlank()) {
            append(". Objects: $topObjects")
        } else if (topText.isNotBlank()) {
            append(". Text: $topText")
        }
    }
}

private fun supportsImageUpload(space: MapSpaceUiModel): Boolean {
    return space.spaceType !in setOf("CORRIDOR", "STAIRCASE", "ELEVATOR", "ESCALATOR")
}

private fun resolveDisplayName(contentResolver: ContentResolver, uri: Uri): String {
    return contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            cursor.getString(nameIndex)
        } else {
            null
        }
    } ?: uri.lastPathSegment ?: "image"
}

private fun createTempImageUri(
    context: android.content.Context,
    direction: UploadDirection,
): Uri {
    val imageDirectory = File(context.cacheDir, "captured_images").apply {
        mkdirs()
    }
    val imageFile = File.createTempFile(
        "${direction.name.lowercase()}_",
        ".jpg",
        imageDirectory,
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile,
    )
}

private fun containsPoint(polygon: List<Offset>, point: Offset): Boolean {
    if (polygon.size < 3) {
        return false
    }

    var contains = false
    var previousIndex = polygon.lastIndex
    for (index in polygon.indices) {
        val current = polygon[index]
        val previous = polygon[previousIndex]

        val intersects = ((current.y > point.y) != (previous.y > point.y)) &&
            (point.x < (previous.x - current.x) * (point.y - current.y) / ((previous.y - current.y).coerceAtLeast(0.0001f)) + current.x)

        if (intersects) {
            contains = !contains
        }
        previousIndex = index
    }

    return contains
}
