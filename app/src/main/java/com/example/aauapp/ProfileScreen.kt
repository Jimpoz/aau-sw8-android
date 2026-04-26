package com.example.aauapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.aauapp.ui.theme.*

@Composable
fun ProfileScreen(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onOpenRoomPhotoUpload: () -> Unit,
    viewModel: UserSessionViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val displayName = uiState.user?.name ?: "John Doe"
    val userEmail = uiState.user?.email ?: "student@aau.dk"

    var avoidStairs by remember { mutableStateOf(true) }
    var voiceGuidance by remember { mutableStateOf(false) }
    var elevatorsOnly by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineLarge,
            color = Slate900
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Blue500, Blue700))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials(displayName),
                    style = MaterialTheme.typography.headlineLarge,
                    color = AndroidCard
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Slate900
                )

                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )

                Text(
                    text = "Gold Member",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Blue600
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "Total Distance",
                value = "4.2 km",
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Steps",
                value = "5,430",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionCard(title = "Navigation Preferences") {
            ToggleRow("Avoid Stairs", avoidStairs) { avoidStairs = it }
            ToggleRow("Voice Guidance", voiceGuidance) { voiceGuidance = it }
            ToggleRow("Use Elevators Only", elevatorsOnly) { elevatorsOnly = it }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionCard(title = "Appearance") {
            ToggleRow("Dark Mode", isDarkMode, onDarkModeChange)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onOpenRoomPhotoUpload,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = AndroidCard,
                contentColor = Blue600
            )
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Upload Room Photos")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                viewModel.logout()
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = AndroidCard,
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Log Out")
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AndroidCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = Slate800
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AndroidCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Slate700,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate50)
                    .padding(12.dp)
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Slate600
        )

        Spacer(modifier = Modifier.weight(1f))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private fun initials(name: String): String {
    return name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .joinToString("")
        .ifBlank { "U" }
}