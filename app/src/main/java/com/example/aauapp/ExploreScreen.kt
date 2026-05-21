package com.example.aauapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Room
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aauapp.data.remote.BuildingMapDto
import com.example.aauapp.data.remote.CampusDto
import com.example.aauapp.data.remote.FloorMapDto
import com.example.aauapp.data.remote.SpaceDisplayDto
import com.example.aauapp.ui.theme.Blue600

private enum class ExploreLevel {
    CAMPUSES,
    BUILDINGS,
    FLOORS,
    ROOMS
}

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = viewModel(),
    onOpenInMap: (floorId: String?, spaceId: String?) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    var level by remember { mutableStateOf(ExploreLevel.CAMPUSES) }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredRooms = remember(uiState.spaces, selectedFilter) {
        when (selectedFilter) {

            "Classroom" -> uiState.spaces.filter {
                it.space_type?.contains("CLASS", true) == true
            }

            "Corridor" -> uiState.spaces.filter {
                it.space_type?.contains("CORRIDOR", true) == true
            }

            "Office" -> uiState.spaces.filter {
                it.space_type?.contains("OFFICE", true) == true
            }

            "Restroom" -> uiState.spaces.filter {
                it.space_type?.contains("REST", true) == true ||
                        it.space_type?.contains("WC", true) == true
            }

            else -> uiState.spaces
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(
            top = 20.dp,
            bottom = 110.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {

            when (level) {

                ExploreLevel.CAMPUSES -> {
                    Header(
                        title = "Explore",
                        subtitle = "Pick a campus to browse its buildings."
                    )
                }

                ExploreLevel.BUILDINGS -> {
                    BuildingsHeader(
                        subtitle = uiState.selectedCampus?.organization_name
                            ?: uiState.selectedCampus?.name,
                        onSwitch = {
                            selectedFilter = "All"
                            level = ExploreLevel.CAMPUSES
                        }
                    )
                }

                ExploreLevel.FLOORS -> {
                    BackHeader(
                        title = uiState.selectedBuilding?.name ?: "Building",
                        onBack = {
                            level = ExploreLevel.BUILDINGS
                        }
                    )
                }

                ExploreLevel.ROOMS -> {
                    BackHeader(
                        title = uiState.selectedFloor?.display_name ?: "Rooms",
                        onBack = {
                            level = ExploreLevel.FLOORS
                        }
                    )
                }
            }
        }

        uiState.error?.let { error ->

            item {
                ErrorCard(error)
            }
        }

        when (level) {

            ExploreLevel.CAMPUSES -> {

                item {
                    SectionTitle("Campuses")
                }

                if (
                    uiState.campuses.isEmpty() &&
                    !uiState.isLoading
                ) {
                    item {
                        EmptyCard("No campuses found.")
                    }
                } else {

                    items(uiState.campuses) { campus ->

                        CampusCard(
                            campus = campus,
                            onClick = {
                                viewModel.selectCampus(campus)
                                level = ExploreLevel.BUILDINGS
                            }
                        )
                    }
                }
            }

            ExploreLevel.BUILDINGS -> {

                item {
                    SectionTitle("Campus")
                }

                uiState.selectedCampus?.let { campus ->

                    item {
                        CampusCard(
                            campus = campus,
                            onClick = {}
                        )
                    }
                }

                item {
                    SectionTitle("Buildings")
                }

                if (
                    uiState.buildings.isEmpty() &&
                    !uiState.isLoading
                ) {
                    item {
                        EmptyCard("No buildings found.")
                    }
                } else {

                    items(uiState.buildings) { building ->

                        BuildingCard(
                            building = building,
                            onClick = {
                                viewModel.selectBuilding(building)
                                level = ExploreLevel.FLOORS
                            },
                            onOpenInMap = {
                                onOpenInMap(buildingDefaultFloorId(building), null)
                            }
                        )
                    }
                }
            }

            ExploreLevel.FLOORS -> {

                item {
                    SectionTitle("Floors")
                }

                if (
                    uiState.floors.isEmpty() &&
                    !uiState.isLoading
                ) {
                    item {
                        EmptyCard("No floors found.")
                    }
                } else {

                    items(uiState.floors) { floor ->

                        FloorCard(
                            floor = floor,
                            onClick = {
                                viewModel.selectFloor(floor)
                                selectedFilter = "All"
                                level = ExploreLevel.ROOMS
                            },
                            onOpenInMap = {
                                onOpenInMap(floor.id, null)
                            }
                        )
                    }
                }
            }

            ExploreLevel.ROOMS -> {

                item {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        FilterChipUi(
                            text = "All",
                            selected = selectedFilter == "All",
                            onClick = {
                                selectedFilter = "All"
                            }
                        )

                        FilterChipUi(
                            text = "Classroom",
                            selected = selectedFilter == "Classroom",
                            onClick = {
                                selectedFilter = "Classroom"
                            }
                        )

                        FilterChipUi(
                            text = "Corridor",
                            selected = selectedFilter == "Corridor",
                            onClick = {
                                selectedFilter = "Corridor"
                            }
                        )
                    }
                }

                item {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        FilterChipUi(
                            text = "Office",
                            selected = selectedFilter == "Office",
                            onClick = {
                                selectedFilter = "Office"
                            }
                        )

                        FilterChipUi(
                            text = "Restroom",
                            selected = selectedFilter == "Restroom",
                            onClick = {
                                selectedFilter = "Restroom"
                            }
                        )
                    }
                }

                if (
                    filteredRooms.isEmpty() &&
                    !uiState.isLoading
                ) {
                    item {
                        EmptyCard("No rooms found.")
                    }
                } else {

                    items(filteredRooms) { room ->

                        RoomCard(
                            room = room,
                            onOpenInMap = {
                                onOpenInMap(uiState.selectedFloor?.id, room.id)
                            }
                        )
                    }
                }
            }
        }

        if (uiState.isLoading) {

            item {
                LoadingCard()
            }
        }
    }
}

@Composable
private fun Header(
    title: String,
    subtitle: String?
) {
    Column {

        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        subtitle?.let {

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun BackHeader(
    title: String,
    onBack: () -> Unit
) {
    Column {

        TextButton(
            onClick = onBack,
            contentPadding = PaddingValues(0.dp)
        ) {

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Blue600
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "Back",
                color = Blue600
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun CampusCard(
    campus: CampusDto,
    onClick: () -> Unit
) {

    ExploreCard(onClick = onClick) {

        IconBubble(Icons.Default.Map)

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = campus.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = campus.organization_name
                    ?: campus.organization_id
                    ?: campus.description
                    ?: "Campus",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun BuildingCard(
    building: BuildingMapDto,
    onClick: () -> Unit,
    onOpenInMap: () -> Unit
) {

    ExploreCard(onClick = onClick) {

        IconBubble(Icons.Default.Business)

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = building.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${building.floor_count ?: building.floors.size} floors",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        NavigationButton(onClick = onOpenInMap)
    }
}

@Composable
private fun FloorCard(
    floor: FloorMapDto,
    onClick: () -> Unit,
    onOpenInMap: () -> Unit
) {

    ExploreCard(onClick = onClick) {

        IconBubble(Icons.Default.Layers)

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = floor.display_name ?: floor.id,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        NavigationButton(onClick = onOpenInMap)
    }
}

@Composable
private fun RoomCard(
    room: SpaceDisplayDto,
    onOpenInMap: () -> Unit
) {

    val icon = if (
        room.space_type?.contains("REST", true) == true ||
        room.space_type?.contains("WC", true) == true
    ) {
        Icons.Default.MeetingRoom
    } else {
        Icons.Default.Room
    }

    ExploreCard(onClick = {}) {

        IconBubble(icon)

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = room.short_name
                    ?: room.display_name
                    ?: room.id,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = when {

                    room.space_type?.contains("REST", true) == true ->
                        "Restroom"

                    room.space_type?.contains("CORRIDOR", true) == true ->
                        "Corridor"

                    room.space_type?.contains("CLASS", true) == true ->
                        "Classroom"

                    room.space_type?.contains("OFFICE", true) == true ->
                        "Office"

                    else -> "Space"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        NavigationButton(onClick = onOpenInMap)
    }
}

@Composable
private fun NavigationButton(onClick: () -> Unit) {

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {

        Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = "Open in map",
            tint = Blue600,
            modifier = Modifier
                .padding(12.dp)
                .size(22.dp)
        )
    }
}

@Composable
private fun BuildingsHeader(
    subtitle: String?,
    onSwitch: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Explore",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Button(
            onClick = onSwitch,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Blue600),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Switch", style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun buildingDefaultFloorId(building: BuildingMapDto): String? {
    val floors = building.floors
    if (floors.isEmpty()) return null
    return (floors.filter { (it.floor_index ?: 0) >= 0 }
        .minByOrNull { it.floor_index ?: Int.MAX_VALUE }
        ?: floors.minByOrNull { it.floor_index ?: Int.MAX_VALUE })?.id
}

@Composable
private fun ExploreCard(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun IconBubble(
    icon: ImageVector
) {

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Blue600,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SectionTitle(
    text: String
) {

    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun FilterChipUi(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(text)
        }
    )
}

@Composable
private fun LoadingCard() {

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            CircularProgressIndicator(
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Loading...",
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ErrorCard(
    text: String
) {

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = text,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun EmptyCard(
    text: String
) {

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}