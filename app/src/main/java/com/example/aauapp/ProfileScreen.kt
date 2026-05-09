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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

    var avoidStairs by remember(profile.avoidStairs) { mutableStateOf(profile.avoidStairs) }
    var voiceEnabled by remember(profile.voiceEnabled) { mutableStateOf(profile.voiceEnabled) }
    var elevatorsOnly by remember(profile.elevatorsOnly) { mutableStateOf(profile.elevatorsOnly) }

    var showEnableMfa by remember { mutableStateOf(false) }
    var showDisableMfa by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var mfaChallengeToken by remember { mutableStateOf<String?>(null) }
    var recoveryCodes by remember { mutableStateOf<List<String>>(emptyList()) }

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

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = AndroidCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Blue500, Blue700))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials(profile.displayName.ifBlank { profile.email }),
                        style = MaterialTheme.typography.headlineMedium,
                        color = AndroidCard
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.displayName.ifBlank { "AAU User" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = Slate900
                    )

                    Text(
                        text = profile.email.ifBlank { "No email" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Blue50
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = Blue600,
                                modifier = Modifier.size(14.dp)
                            )

                            Spacer(modifier = Modifier.width(5.dp))

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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard("Total Distance", "${profile.totalDistanceKm} km", Modifier.weight(1f))
            StatCard("Steps", "%,d".format(profile.steps), Modifier.weight(1f))
        }


        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "Navigation Preferences") {
            ToggleRow(
                title = "Avoid Stairs",
                checked = avoidStairs,
                onCheckedChange = {
                    avoidStairs = it
                    viewModel.updateNavigationPreferences(avoidStairs, voiceEnabled, elevatorsOnly)
                }
            )

            ToggleRow(
                title = "Voice Guidance",
                checked = voiceEnabled,
                onCheckedChange = {
                    voiceEnabled = it
                    viewModel.updateNavigationPreferences(avoidStairs, voiceEnabled, elevatorsOnly)
                }
            )

            ToggleRow(
                title = "Use Elevators Only",
                checked = elevatorsOnly,
                onCheckedChange = {
                    elevatorsOnly = it
                    viewModel.updateNavigationPreferences(avoidStairs, voiceEnabled, elevatorsOnly)
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "Security") {
            ToggleRow(
                title = "Two-Factor Authentication",
                checked = profile.mfaEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) showEnableMfa = true else showDisableMfa = true
                }
            )

            ActionRow(
                icon = Icons.Default.Lock,
                title = "Password",
                subtitle = "Recover or change your password.",
                onClick = { showPasswordDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "Appearance") {
            ToggleRow("Dark Mode", isDarkMode, onDarkModeChange)
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "Current Map Selection") {
            InfoRow("Building", profile.buildingId ?: "Not selected")
            InfoRow("Floor", profile.defaultFloorId ?: "Not selected")
        }

        uiState.message?.let {
            Spacer(modifier = Modifier.height(12.dp))
            MessageCard(message = it, isError = false)
        }

        uiState.error?.let {
            Spacer(modifier = Modifier.height(12.dp))
            MessageCard(message = it, isError = true)
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = onOpenRoomPhotoUpload,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = AndroidCard,
                contentColor = Blue600
            )
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Upload Room Photos")
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = { viewModel.logout() },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
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

    if (showEnableMfa) {
        EnableMfaDialog(
            isLoading = uiState.isLoading,
            error = uiState.error,
            challengeToken = mfaChallengeToken,
            recoveryCodes = recoveryCodes,
            onStart = {
                viewModel.enableEmailMfa { challenge, codes ->
                    mfaChallengeToken = challenge
                    recoveryCodes = codes
                }
            },
            onConfirm = { code ->
                val challenge = mfaChallengeToken
                if (challenge != null) {
                    viewModel.confirmEmailMfa(challenge, code)
                    showEnableMfa = false
                }
            },
            onDismiss = { showEnableMfa = false }
        )
    }

    if (showDisableMfa) {
        DisableMfaDialog(
            isLoading = uiState.isLoading,
            error = uiState.error,
            onConfirm = { password ->
                viewModel.disableMfa(password)
                showDisableMfa = false
            },
            onDismiss = { showDisableMfa = false }
        )
    }

    if (showPasswordDialog) {
        PasswordDialog(
            email = profile.email,
            isLoading = uiState.isLoading,
            error = uiState.error,
            onChangePassword = { current, new ->
                viewModel.changePassword(current, new)
                showPasswordDialog = false
            },
            onForgotPassword = { email ->
                viewModel.forgotPassword(email)
            },
            onResetPassword = { email, code, new ->
                viewModel.resetPassword(email, code, new)
                showPasswordDialog = false
            },
            onDismiss = { showPasswordDialog = false }
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AndroidCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = Slate400)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = Slate800, maxLines = 1)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AndroidCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate50)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = Slate700)
        Spacer(modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Blue50),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Blue600, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Slate700)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Slate500)
        }

        Text("›", style = MaterialTheme.typography.titleLarge, color = Slate400)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = Slate700)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Slate500)
    }
}

@Composable
private fun MessageCard(message: String, isError: Boolean) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else Blue50
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = if (isError) MaterialTheme.colorScheme.error else Blue700,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EnableMfaDialog(
    isLoading: Boolean,
    error: String?,
    challengeToken: String?,
    recoveryCodes: List<String>,
    onStart: () -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable Two-Factor Authentication") },
        text = {
            Column {
                Text("Start setup to receive a verification code by email.")
                Spacer(modifier = Modifier.height(12.dp))

                if (challengeToken != null) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Verification code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (recoveryCodes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Recovery codes:", style = MaterialTheme.typography.labelMedium)
                        recoveryCodes.take(5).forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isLoading,
                onClick = {
                    if (challengeToken == null) onStart() else onConfirm(code)
                }
            ) {
                Text(if (challengeToken == null) "Send Code" else "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DisableMfaDialog(
    isLoading: Boolean,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Disable Two-Factor Authentication") },
        text = {
            Column {
                Text("Enter your password to disable two-factor authentication.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(enabled = !isLoading, onClick = { onConfirm(password) }) {
                Text("Disable")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PasswordDialog(
    email: String,
    isLoading: Boolean,
    error: String?,
    onChangePassword: (String, String) -> Unit,
    onForgotPassword: (String) -> Unit,
    onResetPassword: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var resetEmail by remember { mutableStateOf(email) }
    var resetCode by remember { mutableStateOf("") }
    var resetNewPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Change password", style = MaterialTheme.typography.titleSmall)

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    enabled = !isLoading,
                    onClick = { onChangePassword(currentPassword, newPassword) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Password")
                }

                HorizontalDivider()

                Text("Recover password", style = MaterialTheme.typography.titleSmall)

                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    enabled = !isLoading,
                    onClick = { onForgotPassword(resetEmail) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send Recovery Code")
                }

                OutlinedTextField(
                    value = resetCode,
                    onValueChange = { resetCode = it },
                    label = { Text("Recovery code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = resetNewPassword,
                    onValueChange = { resetNewPassword = it },
                    label = { Text("New password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    enabled = !isLoading,
                    onClick = { onResetPassword(resetEmail, resetCode, resetNewPassword) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Password")
                }

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun initials(name: String): String {
    return name
        .split(" ", "@", ".", "_", "-")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .joinToString("")
        .ifBlank { "U" }
}