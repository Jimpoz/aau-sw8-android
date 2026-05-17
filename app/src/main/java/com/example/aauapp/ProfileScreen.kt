package com.example.aauapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    val profile = uiState.profile

    var avoidStairs by remember(profile.avoidStairs) {
        mutableStateOf(profile.avoidStairs)
    }

    var voiceEnabled by remember(profile.voiceEnabled) {
        mutableStateOf(profile.voiceEnabled)
    }

    var elevatorsOnly by remember(profile.elevatorsOnly) {
        mutableStateOf(profile.elevatorsOnly)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineLarge,
            color = Slate900
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = AndroidCard
            )
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Blue500,
                                    Blue700
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials(
                            profile.displayName.ifBlank {
                                profile.email
                            }
                        ),
                        style = MaterialTheme.typography.headlineMedium,
                        color = AndroidCard
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = profile.displayName.ifBlank { "AAU User" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = Slate900
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = profile.email.ifBlank { "No email" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Blue50
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 5.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = Blue600,
                                modifier = Modifier.size(14.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = profile.role ?: profile.membershipTier,
                                style = MaterialTheme.typography.labelMedium,
                                color = Blue700
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            StatCard(
                title = "Total Distance",
                value = "${profile.totalDistanceKm} km",
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Steps",
                value = "%,d".format(profile.steps),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "Navigation Preferences") {

            ToggleRow(
                title = "Avoid Stairs",
                checked = avoidStairs
            ) {
                avoidStairs = it

                viewModel.updateNavigationPreferences(
                    avoidStairs,
                    voiceEnabled,
                    elevatorsOnly
                )
            }

            ToggleRow(
                title = "Voice Guidance",
                checked = voiceEnabled
            ) {
                voiceEnabled = it

                viewModel.updateNavigationPreferences(
                    avoidStairs,
                    voiceEnabled,
                    elevatorsOnly
                )
            }

            ToggleRow(
                title = "Use Elevators Only",
                checked = elevatorsOnly
            ) {
                elevatorsOnly = it

                viewModel.updateNavigationPreferences(
                    avoidStairs,
                    voiceEnabled,
                    elevatorsOnly
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "Appearance") {

            ToggleRow(
                title = "Dark Mode",
                checked = isDarkMode,
                onCheckedChange = onDarkModeChange
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "Current Map Selection") {

            InfoRow(
                title = "Building",
                value = profile.buildingId ?: "Not selected"
            )

            Spacer(modifier = Modifier.height(10.dp))

            InfoRow(
                title = "Floor",
                value = profile.defaultFloorId ?: "Not selected"
            )
        }

        uiState.message?.let {
            Spacer(modifier = Modifier.height(12.dp))
            MessageCard(it, false)
        }

        uiState.error?.let {
            Spacer(modifier = Modifier.height(12.dp))
            MessageCard(it, true)
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedButton(
            onClick = onOpenRoomPhotoUpload,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = AndroidCard,
                contentColor = Blue600
            )
        ) {

            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text("Upload Room Photos")
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                viewModel.logout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = AndroidCard,
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {

            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text("Log Out")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                showDeleteDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = AndroidCard
            )
        ) {

            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text("Delete Profile")
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    if (showDeleteDialog) {

        DeleteProfileDialog(
            isLoading = uiState.isLoading,
            error = uiState.error,
            onConfirm = { password ->
                viewModel.deleteProfile(password)
                showDeleteDialog = false
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = AndroidCard
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = AndroidCard
        )
    ) {

        Column {

            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate50)
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
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
            color = Slate700
        )

        Spacer(modifier = Modifier.weight(1f))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun InfoRow(
    title: String,
    value: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Slate700
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Slate500
        )
    }
}

@Composable
private fun MessageCard(
    message: String,
    isError: Boolean
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
            } else {
                Blue50
            }
        )
    ) {

        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                Blue700
            }
        )
    }
}

@Composable
private fun DeleteProfileDialog(
    isLoading: Boolean,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {

    var password by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Delete Profile")
        },

        text = {

            Column {

                Text(
                    "This permanently deletes your account and cannot be undone."
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                    },
                    label = {
                        Text("Password")
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },

        confirmButton = {

            Button(
                enabled = !isLoading,
                onClick = {
                    onConfirm(password)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = AndroidCard
                )
            ) {

                Text("Delete")
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text("Cancel")
            }
        }
    )
}

private fun initials(name: String): String {

    return name
        .split(" ", "@", ".", "_", "-")
        .filter {
            it.isNotBlank()
        }
        .take(2)
        .mapNotNull {
            it.firstOrNull()?.uppercaseChar()?.toString()
        }
        .joinToString("")
        .ifBlank { "U" }
}