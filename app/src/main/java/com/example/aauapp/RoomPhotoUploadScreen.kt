package com.example.aauapp

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.aauapp.data.remote.ApiModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun RoomPhotoUploadScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoadingRooms by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    var rooms by remember { mutableStateOf<List<String>>(emptyList()) }
    var manualRoom by remember { mutableStateOf(false) }
    var roomName by remember { mutableStateOf("") }

    var selectedDirection by remember { mutableStateOf("") }

    var northUri by remember { mutableStateOf<Uri?>(null) }
    var eastUri by remember { mutableStateOf<Uri?>(null) }
    var southUri by remember { mutableStateOf<Uri?>(null) }
    var westUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            when (selectedDirection) {
                "North" -> northUri = uri
                "East" -> eastUri = uri
                "South" -> southUri = uri
                "West" -> westUri = uri
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            rooms = ApiModule.backendApi.getRoomSummaryRooms().names
            roomName = rooms.firstOrNull().orEmpty()
        } catch (e: Exception) {
            error = e.message ?: "Failed to load rooms"
        } finally {
            isLoadingRooms = false
        }
    }

    val canUpload =
        roomName.isNotBlank() &&
                northUri != null &&
                eastUri != null &&
                southUri != null &&
                westUri != null &&
                !isUploading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6FA))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp)
    ) {
        Spacer(Modifier.height(58.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("Close", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Upload Room Photos",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black
            )

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(68.dp))
        }

        Spacer(Modifier.height(34.dp))

        UploadCard {
            Text(
                text = "How it works",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF334155)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Stand in the center of the room and take four photos, one per compass direction. Upload them clockwise: North → East → South → West. The server runs object detection on all four and stores the result on the room.",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF526071)
            )
        }

        Spacer(Modifier.height(24.dp))

        UploadCard {
            Text(
                text = "Room",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF334155)
            )

            Spacer(Modifier.height(16.dp))

            if (isLoadingRooms) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 3.dp,
                        color = Color(0xFF8AB4FF)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "Loading rooms...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF6B778C)
                    )
                }
            } else if (!manualRoom && rooms.isNotEmpty()) {
                RoomDropdown(
                    rooms = rooms,
                    selected = roomName,
                    onSelected = { roomName = it }
                )
            }

            if (manualRoom || rooms.isEmpty()) {
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    placeholder = { Text("Room name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Type a room name manually",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF526071),
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = manualRoom,
                    onCheckedChange = {
                        manualRoom = it
                        if (!it && rooms.isNotEmpty()) {
                            roomName = rooms.first()
                        } else {
                            roomName = ""
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        UploadCard {
            Text(
                text = "Photos",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF334155)
            )

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PhotoPickerBox(
                    label = "North",
                    uri = northUri,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedDirection = "North"
                        picker.launch("image/*")
                    }
                )

                PhotoPickerBox(
                    label = "East",
                    uri = eastUri,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedDirection = "East"
                        picker.launch("image/*")
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PhotoPickerBox(
                    label = "South",
                    uri = southUri,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedDirection = "South"
                        picker.launch("image/*")
                    }
                )

                PhotoPickerBox(
                    label = "West",
                    uri = westUri,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedDirection = "West"
                        picker.launch("image/*")
                    }
                )
            }
        }

        error?.let {
            Spacer(Modifier.height(16.dp))
            MessageCard(text = it, isError = true)
        }

        message?.let {
            Spacer(Modifier.height(16.dp))
            MessageCard(text = it, isError = false)
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                val n = northUri
                val e = eastUri
                val s = southUri
                val w = westUri

                if (n == null || e == null || s == null || w == null) return@Button

                scope.launch {
                    isUploading = true
                    error = null
                    message = null

                    try {
                        val response = withContext(Dispatchers.IO) {
                            ApiModule.backendApi.uploadRoomPhotos(
                                roomName = roomName.toRequestBody("text/plain".toMediaTypeOrNull()),
                                north_image = context.uriToPart("north_image", n, "north.jpg"),
                                east_image = context.uriToPart("east_image", e, "east.jpg"),
                                south_image = context.uriToPart("south_image", s, "south.jpg"),
                                west_image = context.uriToPart("west_image", w, "west.jpg")
                            )
                        }

                        message = "Uploaded ${response.stored_image_count} photos for ${response.room_name}"
                    } catch (ex: Exception) {
                        error = ex.message ?: "Upload failed"
                    } finally {
                        isUploading = false
                    }
                }
            },
            enabled = canUpload,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2F6FEA),
                disabledContainerColor = Color(0xFFB8C0CE),
                contentColor = Color.White,
                disabledContentColor = Color.White
            )
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Upload & Analyze",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun UploadCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
private fun PhotoPickerBox(
    label: String,
    uri: Uri?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .background(Color(0xFFF0F3F8), RoundedCornerShape(18.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = Color(0xFF66788F),
                    modifier = Modifier.size(38.dp)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (uri == null) "Tap to add" else "Selected",
                    color = Color(0xFF66788F),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF334155)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomDropdown(
    rooms: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(18.dp),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            rooms.forEach { room ->
                DropdownMenuItem(
                    text = { Text(room) },
                    onClick = {
                        onSelected(room)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MessageCard(
    text: String,
    isError: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                Color(0xFFFFE5E5)
            } else {
                Color(0xFFEAF3FF)
            }
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            color = if (isError) {
                Color(0xFFB00020)
            } else {
                Color(0xFF2563EB)
            }
        )
    }
}

private fun android.content.Context.uriToPart(
    partName: String,
    uri: Uri,
    fileName: String
): MultipartBody.Part {
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: error("Could not read image")

    val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())

    return MultipartBody.Part.createFormData(
        partName,
        fileName,
        body
    )
}