package com.example.aauapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.aauapp.data.remote.FloorMapDto
import com.example.aauapp.data.remote.FloorPlanRepository
import com.example.aauapp.data.remote.PositioningRepository
import com.example.aauapp.data.remote.SpaceDisplayDto
import kotlinx.coroutines.launch

private fun wifiScanPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.NEARBY_WIFI_DEVICES
    } else {
        Manifest.permission.ACCESS_FINE_LOCATION
    }

@Composable
fun PositioningControls(
    floorId: String,
    spaces: List<SpaceDisplayDto>,
    buildingId: String?,
    floors: List<FloorMapDto>,
    canCalibrate: Boolean,
    onLocated: (spaceId: String, floorId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val manager = remember { PositioningManager(context) }
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var showCalibration by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val action = pendingAction
        pendingAction = null
        if (granted) action?.invoke()
    }

    fun withPermission(action: () -> Unit) {
        val perm = wifiScanPermission()
        val granted = ContextCompat.checkSelfPermission(context, perm) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            action()
        } else {
            pendingAction = action
            permLauncher.launch(perm)
        }
    }

    fun findMe() {
        if (floorId.isBlank()) return
        busy = true
        scope.launch {
            val result = manager.locate(floorId)
            busy = false
            val spaceId = result?.space_id
            if (spaceId != null) onLocated(spaceId, floorId)
        }
    }

    Column(modifier = modifier, horizontalAlignment = androidx.compose.ui.Alignment.End) {
        if (canCalibrate) {
            OutlinedButton(
                onClick = { withPermission { showCalibration = true } },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(Icons.Default.Tune, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Calibrate Wi-Fi")
            }
            Spacer(Modifier.height(10.dp))
        }

        Button(
            onClick = { withPermission { findMe() } },
            enabled = !busy,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6C3BEA),
                contentColor = Color.White
            )
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Sensors, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text("Find me (Wi-Fi)")
        }
    }

    if (showCalibration) {
        WifiCalibrationSheet(
            floorId = floorId,
            spaces = spaces,
            floors = floors,
            manager = manager,
            onDismiss = { showCalibration = false }
        )
    }
}

private data class CalibrationRoom(
    val space: SpaceDisplayDto,
    val floorId: String,
    val floorName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WifiCalibrationSheet(
    floorId: String,
    spaces: List<SpaceDisplayDto>,
    floors: List<FloorMapDto>,
    manager: PositioningManager,
    onDismiss: () -> Unit
) {
    val repo = remember { PositioningRepository() }
    val floorRepo = remember { FloorPlanRepository() }
    val scope = rememberCoroutineScope()

    var rooms by remember { mutableStateOf<List<CalibrationRoom>>(emptyList()) }
    var selectedSpaceId by remember { mutableStateOf<String?>(null) }
    var counts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var loadingRooms by remember { mutableStateOf(true) }

    LaunchedEffect(floors, floorId) {
        loadingRooms = true
        val collected = mutableListOf<CalibrationRoom>()
        val mergedCounts = mutableMapOf<String, Int>()
        if (floors.isNotEmpty()) {
            floors.forEach { f ->
                val fid = f.id
                val label = f.display_name ?: f.id
                runCatching { floorRepo.getFloorDisplay(fid) }.getOrNull()?.forEach { s ->
                    collected.add(CalibrationRoom(s, fid, label))
                }
                runCatching { repo.floorSurvey(fid) }.getOrNull()?.per_space_counts?.let {
                    mergedCounts.putAll(it)
                }
            }
        } else {
            spaces.forEach { s -> collected.add(CalibrationRoom(s, floorId, "")) }
            runCatching { repo.floorSurvey(floorId) }.getOrNull()?.per_space_counts?.let {
                mergedCounts.putAll(it)
            }
        }
        rooms = collected.sortedBy { r ->
            (r.space.display_name ?: r.space.short_name ?: r.space.id).lowercase()
        }
        counts = mergedCounts
        selectedSpaceId = collected.firstOrNull()?.space?.id
        loadingRooms = false
    }

    val selectedRoom = rooms.firstOrNull { it.space.id == selectedSpaceId }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Calibrate Wi-Fi positioning",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Stand inside the room and capture a few samples. More samples per " +
                    "room means more accurate positioning later. Only rooms in this " +
                    "building are shown.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            RoomPicker(
                rooms = rooms,
                selectedSpaceId = selectedSpaceId,
                loading = loadingRooms,
                onSelect = { selectedSpaceId = it }
            )

            selectedSpaceId?.let { sid ->
                Text(
                    "Samples stored for this room: ${counts[sid] ?: 0}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            status?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = {
                    val room = selectedRoom ?: return@Button
                    busy = true
                    status = "Scanning Wi-Fi…"
                    scope.launch {
                        val res = manager.collectFingerprint(room.space.id, room.floorId)
                        busy = false
                        if (res != null) {
                            status = "Saved a sample for this room."
                            counts = counts.toMutableMap().apply {
                                put(room.space.id, (counts[room.space.id] ?: 0) + 1)
                            }
                        } else {
                            status = "No Wi-Fi visible, or save failed."
                        }
                    }
                },
                enabled = !busy && selectedRoom != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (busy) "Capturing…" else "Capture sample")
            }
        }
    }
}

@Composable
private fun RoomPicker(
    rooms: List<CalibrationRoom>,
    selectedSpaceId: String?,
    loading: Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val multiFloor = rooms.map { it.floorId }.distinct().size > 1
    val selected = rooms.firstOrNull { it.space.id == selectedSpaceId }
    val selectedText = selected?.let { roomLabel(it, multiFloor) }

    val placeholder = when {
        loading -> "Loading rooms…"
        rooms.isEmpty() -> "No rooms in this building"
        else -> "Choose a room…"
    }

    Box {
        OutlinedButton(
            onClick = { if (rooms.isNotEmpty()) expanded = true },
            enabled = rooms.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedText ?: placeholder,
                modifier = Modifier.weight(1f),
                color = if (selectedText == null) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            rooms.forEach { room ->
                DropdownMenuItem(
                    text = { Text(roomLabel(room, multiFloor)) },
                    onClick = {
                        onSelect(room.space.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun roomLabel(room: CalibrationRoom, multiFloor: Boolean): String {
    val name = room.space.display_name ?: room.space.short_name ?: room.space.id
    return if (multiFloor && room.floorName.isNotBlank()) "$name · ${room.floorName}" else name
}
