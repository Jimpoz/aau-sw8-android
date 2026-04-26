package com.example.aauapp

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

private enum class CompassDirection(val label: String) {
    North("North"),
    East("East"),
    South("South"),
    West("West")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomPhotoUploadScreen(
    viewModel: RoomPhotoUploadViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var dialogDirection by remember { mutableStateOf<CompassDirection?>(null) }
    var pendingCameraDirection by remember { mutableStateOf<CompassDirection?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val northGalleryPicker = rememberImagePicker { viewModel.setNorthImage(it) }
    val eastGalleryPicker = rememberImagePicker { viewModel.setEastImage(it) }
    val southGalleryPicker = rememberImagePicker { viewModel.setSouthImage(it) }
    val westGalleryPicker = rememberImagePicker { viewModel.setWestImage(it) }

    val cameraLauncher = rememberLauncherForActivityResult(TakePicture()) { success ->
        if (success) {
            when (pendingCameraDirection) {
                CompassDirection.North -> viewModel.setNorthImage(pendingCameraUri)
                CompassDirection.East -> viewModel.setEastImage(pendingCameraUri)
                CompassDirection.South -> viewModel.setSouthImage(pendingCameraUri)
                CompassDirection.West -> viewModel.setWestImage(pendingCameraUri)
                null -> Unit
            }
        }
        pendingCameraDirection = null
        pendingCameraUri = null
    }

    fun launchGallery(direction: CompassDirection) {
        when (direction) {
            CompassDirection.North -> northGalleryPicker.launch("image/*")
            CompassDirection.East -> eastGalleryPicker.launch("image/*")
            CompassDirection.South -> southGalleryPicker.launch("image/*")
            CompassDirection.West -> westGalleryPicker.launch("image/*")
        }
    }

    fun launchCamera(direction: CompassDirection) {
        val uri = createTempImageUri(context)
        pendingCameraDirection = direction
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Upload Room Photos",
            style = MaterialTheme.typography.headlineSmall
        )

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }

        InfoCard(
            title = "How it works",
            body = "Stand near the center of the room and add four photos: North, East, South, and West. Then upload them for analysis."
        )

        if (uiState.isLoading) {
            CircularProgressIndicator()
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Room",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = uiState.selectedRoomName,
                    onValueChange = { viewModel.setRoomName(it) },
                    label = { Text("Room name") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.rooms.isNotEmpty()) {
                    Text(
                        text = "Known rooms: " + uiState.rooms.joinToString { it.room_name },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Photos",
                    style = MaterialTheme.typography.titleMedium
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 2
                ) {
                    DirectionTile(
                        title = "North",
                        uri = uiState.northImage,
                        onClick = { dialogDirection = CompassDirection.North }
                    )
                    DirectionTile(
                        title = "East",
                        uri = uiState.eastImage,
                        onClick = { dialogDirection = CompassDirection.East }
                    )
                    DirectionTile(
                        title = "South",
                        uri = uiState.southImage,
                        onClick = { dialogDirection = CompassDirection.South }
                    )
                    DirectionTile(
                        title = "West",
                        uri = uiState.westImage,
                        onClick = { dialogDirection = CompassDirection.West }
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.upload(context.contentResolver) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isUploading
        ) {
            Text(if (uiState.isUploading) "Uploading..." else "Upload & Analyze")
        }

        uiState.successMessage?.let {
            ResultCard(title = "Upload Result") {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (uiState.roomObjects.isNotEmpty()) {
            ResultCard(title = "Detected Objects") {
                uiState.roomObjects.forEach { (name, count) ->
                    Text("$name ($count)")
                }
            }
        }

        if (uiState.roomText.isNotEmpty()) {
            ResultCard(title = "Detected Text") {
                uiState.roomText.forEach { (name, count) ->
                    Text("$name ($count)")
                }
            }
        }

        if (uiState.storedViews.isNotEmpty()) {
            ResultCard(title = "Stored Views") {
                Text(uiState.storedViews.joinToString())
            }
        }

        if (uiState.summaries.isNotEmpty()) {
            ResultCard(title = "View Summaries") {
                uiState.summaries.forEach { summary ->
                    val direction = summary.direction ?: "Unknown"
                    Text(
                        text = "$direction: ${summary.summary ?: "No summary"}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (summary.object_counts.isNotEmpty()) {
                        Text(
                            text = "Objects: " + summary.object_counts.entries.joinToString {
                                "${it.key} (${it.value})"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (summary.text_counts.isNotEmpty()) {
                        Text(
                            text = "Text: " + summary.text_counts.entries.joinToString {
                                "${it.key} (${it.value})"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }

        uiState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    dialogDirection?.let { direction ->
        AlertDialog(
            onDismissRequest = { dialogDirection = null },
            title = { Text("Add ${direction.label} photo") },
            text = { Text("Choose photo source") },
            confirmButton = {
                TextButton(
                    onClick = {
                        launchCamera(direction)
                        dialogDirection = null
                    }
                ) {
                    Text("Take Photo")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            launchGallery(direction)
                            dialogDirection = null
                        }
                    ) {
                        Text("Gallery")
                    }

                    TextButton(
                        onClick = { dialogDirection = null }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ResultCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            content()
        }
    }
}

@Composable
private fun DirectionTile(
    title: String,
    uri: Uri?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.48f)
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                HorizontalDivider()
                Text(
                    text = uri?.lastPathSegment ?: "Tap to add",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun rememberImagePicker(onResult: (Uri?) -> Unit) =
    rememberLauncherForActivityResult(GetContent()) { uri ->
        onResult(uri)
    }

private fun createTempImageUri(context: Context): Uri {
    val file = File.createTempFile(
        "room_upload_",
        ".jpg",
        context.cacheDir
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
}