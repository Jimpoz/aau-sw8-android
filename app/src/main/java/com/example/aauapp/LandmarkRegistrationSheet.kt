package com.example.aauapp

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aauapp.data.remote.FloorMapDto
import com.example.aauapp.data.remote.FloorPlanRepository
import com.example.aauapp.data.remote.LandmarkRepository
import com.example.aauapp.data.remote.SpaceDisplayDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LandmarkRegUiState(
    val floors: List<FloorMapDto> = emptyList(),
    val selectedFloorId: String? = null,
    val spaces: List<SpaceDisplayDto> = emptyList(),
    val isLoadingSpaces: Boolean = false,
    val selectedSpaceId: String? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

class LandmarkRegistrationViewModel : ViewModel() {

    private val floorRepo = FloorPlanRepository()
    private val landmarkRepo = LandmarkRepository()
    private val spacesCache = mutableMapOf<String, List<SpaceDisplayDto>>()

    private val _uiState = MutableStateFlow(LandmarkRegUiState())
    val uiState: StateFlow<LandmarkRegUiState> = _uiState.asStateFlow()

    private var started = false

    fun start(buildingId: String?, preferredFloorId: String?) {
        if (started) return
        started = true

        if (buildingId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "No building selected — open the Map tab and pick a building first."
            )
            return
        }

        viewModelScope.launch {
            try {
                val floors = floorRepo.getBuildingFloors(buildingId)
                val initial = floors.firstOrNull { it.id == preferredFloorId }?.id
                    ?: floors.firstOrNull { it.floor_index == 0 }?.id
                    ?: floors.firstOrNull()?.id
                _uiState.value = _uiState.value.copy(floors = floors, selectedFloorId = initial)
                initial?.let { loadSpaces(it) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not load floors")
            }
        }
    }

    fun selectFloor(floorId: String) {
        _uiState.value = _uiState.value.copy(selectedFloorId = floorId, selectedSpaceId = null)
        loadSpaces(floorId)
    }

    fun selectSpace(spaceId: String) {
        _uiState.value = _uiState.value.copy(selectedSpaceId = spaceId)
    }

    private fun loadSpaces(floorId: String) {
        spacesCache[floorId]?.let {
            _uiState.value = _uiState.value.copy(spaces = it, isLoadingSpaces = false)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSpaces = true, spaces = emptyList())
            try {
                val spaces = floorRepo.getFloorDisplay(floorId)
                spacesCache[floorId] = spaces
                if (_uiState.value.selectedFloorId == floorId) {
                    _uiState.value = _uiState.value.copy(spaces = spaces, isLoadingSpaces = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingSpaces = false,
                    error = e.message ?: "Could not load rooms"
                )
            }
        }
    }

    fun submit(name: String, jpeg: ByteArray) {
        val spaceId = _uiState.value.selectedSpaceId ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Enter a name")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null, info = null)
            try {
                landmarkRepo.register(trimmed, spaceId, jpeg)
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    info = "Registered — ml-vision will pick this up on the next stream connection."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "Registration failed"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandmarkRegistrationSheet(
    jpeg: ByteArray,
    buildingId: String?,
    preferredFloorId: String?,
    onDismiss: () -> Unit,
    onRegistered: () -> Unit = {},
    viewModel: LandmarkRegistrationViewModel = viewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.start(buildingId, preferredFloorId)
    }

    LaunchedEffect(ui.info) {
        if (ui.info != null) {
            delay(1200)
            onRegistered()
            onDismiss()
        }
    }

    val bitmap = remember(jpeg) { BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Register landmark",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Captured frame",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }

            Text(
                text = "Make sure the object you want to recognise (poster, sign, placard) " +
                        "fills most of the frame. Texture-rich 2D surfaces work best.",
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Landmark name") },
                placeholder = { Text("e.g. Joker poster") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            PickerField(
                label = "Floor",
                selectedText = ui.floors.firstOrNull { it.id == ui.selectedFloorId }
                    ?.let { it.display_name ?: "Floor ${it.floor_index ?: 0}" },
                placeholder = if (ui.floors.isEmpty()) "No floors loaded" else "Choose a floor…",
                enabled = ui.floors.isNotEmpty(),
                items = ui.floors.map { it.id to (it.display_name ?: "Floor ${it.floor_index ?: 0}") },
                onSelect = { viewModel.selectFloor(it) }
            )

            PickerField(
                label = if (ui.isLoadingSpaces) "Attach to space (loading…)" else "Attach to space",
                selectedText = ui.spaces.firstOrNull { it.id == ui.selectedSpaceId }
                    ?.let { it.display_name ?: it.short_name ?: it.id },
                placeholder = if (ui.spaces.isEmpty()) "No rooms on this floor" else "Choose a room…",
                enabled = ui.spaces.isNotEmpty(),
                items = ui.spaces.map { it.id to (it.display_name ?: it.short_name ?: it.id) },
                onSelect = { viewModel.selectSpace(it) }
            )

            ui.info?.let {
                Text(it, color = Color(0xFF1B9E4B), style = MaterialTheme.typography.bodyMedium)
            }
            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            val canSubmit = !ui.isSubmitting && name.isNotBlank() && ui.selectedSpaceId != null
            Button(
                onClick = { viewModel.submit(name, jpeg) },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (ui.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (ui.isSubmitting) "Registering…" else "Register landmark")
            }
        }
    }
}

@Composable
private fun PickerField(
    label: String,
    selectedText: String?,
    placeholder: String,
    enabled: Boolean,
    items: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(6.dp))
        Box {
            OutlinedButton(
                onClick = { if (enabled) expanded = true },
                enabled = enabled,
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
                items.forEach { (id, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            onSelect(id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
