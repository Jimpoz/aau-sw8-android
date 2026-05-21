package com.example.aauapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.util.Locale
import com.example.aauapp.ui.theme.Blue500
import com.example.aauapp.ui.theme.Blue700

@Composable
fun ProfileScreen(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onOpenRoomPhotoUpload: () -> Unit,
    viewModel: UserSessionViewModel
) {

    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile

    val context = LocalContext.current
    val activityTracker = remember {
        (context.applicationContext as AAUAppApplication).appContainer.activityTracker
    }
    LaunchedEffect(Unit) { activityTracker.start() }
    val todayDistanceM by activityTracker.todayDistanceM.collectAsState()
    val todaySteps by activityTracker.todaySteps.collectAsState()
    val distanceText = if (todayDistanceM >= 1000.0) {
        String.format(Locale.US, "%.1f km", todayDistanceM / 1000.0)
    } else {
        String.format(Locale.US, "%.0f m", todayDistanceM)
    }

    var wheelchairOnly by remember {
        mutableStateOf(profile.wheelchairOnly)
    }

    var avoidStairs by remember {
        mutableStateOf(profile.avoidStairs)
    }

    var elevatorsOnly by remember {
        mutableStateOf(profile.elevatorsOnly)
    }

    var showDelete by remember { mutableStateOf(false) }
    var showDisableMfa by remember { mutableStateOf(false) }
    var showMfaSheet by remember { mutableStateOf(false) }
    var showPasswordSheet by remember { mutableStateOf(false) }

    val pageBg = MaterialTheme.colorScheme.background
    val titleColor = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val red = Color(0xFFFF3B30)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp)
    ) {

        Spacer(Modifier.height(72.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(92.dp)
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
                    ).lowercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall
                )
            }

            Spacer(Modifier.width(18.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = profile.email,
                    color = titleColor,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "${profile.role ?: "Member"} • ${profile.organizationId ?: "local"}",
                    color = muted
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            StatCard(
                title = "TODAY'S DISTANCE",
                value = distanceText,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "TODAY'S STEPS",
                value = todaySteps.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        IosSection("Navigation Preferences") {

            PreferenceRow(
                title = "Wheelchair Accessible Only",
                subtitle = "Route only through spaces marked accessible",
                checked = wheelchairOnly,
                onChange = {
                    wheelchairOnly = it
                    viewModel.updateNavigationPreferences(
                        avoidStairs = avoidStairs,
                        voiceEnabled = false,
                        elevatorsOnly = elevatorsOnly,
                        wheelchairOnly = wheelchairOnly
                    )
                }
            )

            IosDivider()

            PreferenceRow(
                title = "Avoid Stairs",
                subtitle = "Excludes staircases and escalators",
                checked = avoidStairs,
                onChange = {
                    avoidStairs = it
                    viewModel.updateNavigationPreferences(
                        avoidStairs = avoidStairs,
                        voiceEnabled = false,
                        elevatorsOnly = elevatorsOnly,
                        wheelchairOnly = wheelchairOnly
                    )
                }
            )

            IosDivider()

            PreferenceRow(
                title = "Use Elevators Only",
                subtitle = "Excludes stairs, escalators, and ramps",
                checked = elevatorsOnly,
                onChange = {
                    elevatorsOnly = it
                    viewModel.updateNavigationPreferences(
                        avoidStairs = avoidStairs,
                        voiceEnabled = false,
                        elevatorsOnly = elevatorsOnly,
                        wheelchairOnly = wheelchairOnly
                    )
                }
            )

            IosDivider()

            PreferenceRow(
                title = "Voice Guidance",
                subtitle = "Coming soon",
                checked = false,
                enabled = false,
                onChange = {}
            )
        }

        Spacer(Modifier.height(22.dp))

        IosSection("Security") {

            SecurityRow(
                title = "Two-factor authentication",
                subtitle = if (profile.mfaEnabled)
                    "On"
                else
                    "Off — recommended for owner/editor accounts",
                action = if (profile.mfaEnabled)
                    "Disable"
                else
                    "Enable",
                destructive = profile.mfaEnabled,
                onClick = {
                    if (profile.mfaEnabled) {
                        showDisableMfa = true
                    } else {
                        showMfaSheet = true
                    }
                }
            )

            IosDivider()

            SecurityRow(
                title = "Password",
                subtitle = "Recover or change your password",
                action = "Recover",
                onClick = {
                    showPasswordSheet = true
                }
            )
        }

        Spacer(Modifier.height(22.dp))

        IosSection("Appearance") {

            PreferenceRow(
                title = "Dark Mode",
                subtitle = null,
                checked = isDarkMode,
                onChange = onDarkModeChange
            )
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = onOpenRoomPhotoUpload,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(22.dp)
        ) {

            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null
            )

            Spacer(Modifier.width(10.dp))

            Text("Upload Room Photos")
        }

        Spacer(Modifier.height(18.dp))

        OutlinedButton(
            onClick = {
                viewModel.logout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = red
            )
        ) {

            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = null
            )

            Spacer(Modifier.width(10.dp))

            Text("Log Out")
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = {
                showDelete = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = red
            )
        ) {

            Icon(
                Icons.Default.Delete,
                contentDescription = null
            )

            Spacer(Modifier.width(10.dp))

            Text("Delete Profile")
        }

        Spacer(Modifier.height(120.dp))
    }

    if (showMfaSheet) {

        MfaEnrollmentSheet(
            email = profile.email,
            viewModel = viewModel,
            isLoading = uiState.isLoading,
            error = uiState.error,
            onDismiss = { showMfaSheet = false }
        )
    }

    if (showPasswordSheet) {

        PasswordSheet(
            email = profile.email,
            isLoading = uiState.isLoading,
            onChangePassword = { current, newPassword ->

                viewModel.changePassword(
                    currentPassword = current,
                    newPassword = newPassword
                )
            },
            onForgotPassword = {
                viewModel.forgotPassword(it)
            },
            onResetPassword = { email, code, password ->

                viewModel.resetPassword(
                    email = email,
                    code = code,
                    newPassword = password
                )
            },
            onDismiss = {
                showPasswordSheet = false
            }
        )
    }

    if (showDisableMfa) {

        PasswordDialog(
            title = "Disable MFA",
            confirmText = "Disable",
            isLoading = uiState.isLoading,
            destructive = true,
            onConfirm = {

                viewModel.disableMfa(it)
                showDisableMfa = false
            },
            onDismiss = {
                showDisableMfa = false
            }
        )
    }

    if (showDelete) {

        PasswordDialog(
            title = "Delete Profile",
            confirmText = "Delete",
            isLoading = uiState.isLoading,
            destructive = true,
            onConfirm = {

                viewModel.deleteProfile(it)
                showDelete = false
            },
            onDismiss = {
                showDelete = false
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier
) {

    Card(
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun IosSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                    .padding(
                        horizontal = 24.dp,
                        vertical = 14.dp
                    )
            ) {

                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Column(
                modifier = Modifier.padding(18.dp),
                content = content
            )
        }
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (subtitle == null) 58.dp else 78.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            subtitle?.let {

                Spacer(Modifier.height(2.dp))

                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onChange
        )
    }
}

@Composable
private fun SecurityRow(
    title: String,
    subtitle: String,
    action: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = action,
            color =
                if (destructive)
                    MaterialTheme.colorScheme.error
                else
                    Color(0xFF2F6FEA)
        )
    }
}

@Composable
private fun IosDivider() {

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.7.dp
    )
}

@Composable
private fun PasswordDialog(
    title: String,
    confirmText: String,
    isLoading: Boolean,
    destructive: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {

    var password by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                },
                label = {
                    Text("Password")
                },
                visualTransformation = PasswordVisualTransformation()
            )
        },
        confirmButton = {

            Button(
                onClick = {
                    onConfirm(password)
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (destructive)
                            Color.Red
                        else
                            Color.Blue
                )
            ) {

                Text(confirmText)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MfaEnrollmentSheet(
    email: String,
    viewModel: UserSessionViewModel,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit
) {
    var method by remember { mutableStateOf("totp") }
    var stage by remember { mutableStateOf("picking") }

    var secret by remember { mutableStateOf<String?>(null) }
    var provisioningUri by remember { mutableStateOf<String?>(null) }
    var emailChallenge by remember { mutableStateOf<String?>(null) }
    var recoveryCodes by remember { mutableStateOf<List<String>>(emptyList()) }

    var savedCodes by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Enable two-factor", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(20.dp))

            when (stage) {
                "picking" -> {
                    Text(
                        "Use an authenticator app (more secure, works offline) or have a " +
                                "one-time code emailed to you on every sign-in.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = method == "totp",
                            onClick = { method = "totp" },
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Authenticator") }
                        SegmentedButton(
                            selected = method == "email",
                            onClick = { method = "email" },
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Email") }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (method == "totp") {
                                viewModel.beginMfaTotp { s, uri, codes ->
                                    secret = s
                                    provisioningUri = uri
                                    recoveryCodes = codes
                                    stage = "ready"
                                }
                            } else {
                                viewModel.beginMfaEmail { challenge, codes ->
                                    emailChallenge = challenge
                                    recoveryCodes = codes
                                    stage = "ready"
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Continue")
                        }
                    }
                }

                "ready" -> {
                    if (method == "totp") {
                        Text("Step 1 — Add to your authenticator", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Scan the QR code below in Google Authenticator, Authy or 1Password, " +
                                    "or type the secret in by hand.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        provisioningUri?.let { uri ->
                            generateQrBitmap(uri)?.let { qr ->
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Image(
                                        bitmap = qr,
                                        contentDescription = "Authenticator QR code",
                                        modifier = Modifier
                                            .size(220.dp)
                                            .background(Color.White, RoundedCornerShape(12.dp))
                                            .padding(10.dp)
                                    )
                                }
                            }
                        }
                        secret?.let {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Or enter the secret manually",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(it, style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        Text("Step 1 — Check your inbox", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "We just emailed a 6-digit code to $email. It expires in a few minutes.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(22.dp))
                    Text("Step 2 — Save your recovery codes", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            recoveryCodes.forEach {
                                Text(it, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("I've saved these codes somewhere safe", modifier = Modifier.weight(1f))
                        Switch(checked = savedCodes, onCheckedChange = { savedCodes = it })
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("Step 3 — Confirm with a code", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.take(6) },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (method == "totp") {
                                viewModel.finishMfaTotp(code) { stage = "done" }
                            } else {
                                viewModel.finishMfaEmail(emailChallenge ?: "", code) { stage = "done" }
                            }
                        },
                        enabled = savedCodes && code.length == 6 && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Confirm and enable")
                        }
                    }
                }

                "done" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF1B9E4B),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("MFA enabled", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (method == "email")
                                "From now on you'll receive a one-time code by email at sign-in."
                            else
                                "From now on, you'll be asked for a 6-digit code at sign-in.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) { Text("Done") }
                    }
                }
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordSheet(
    email: String,
    isLoading: Boolean,
    onChangePassword: (String, String) -> Unit,
    onForgotPassword: (String) -> Unit,
    onResetPassword: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {

    var selectedTab by remember {
        mutableStateOf(0)
    }

    var currentPassword by remember {
        mutableStateOf("")
    }

    var newPassword by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var recoveryCode by remember {
        mutableStateOf("")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {

            Text(
                "Password",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(24.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {

                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Change")
                }

                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Email reset")
                }
            }

            Spacer(Modifier.height(24.dp))

            if (selectedTab == 0) {

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                    },
                    placeholder = {
                        Text("Current password")
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                    },
                    placeholder = {
                        Text("New password")
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                    },
                    placeholder = {
                        Text("Confirm password")
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {

                        onChangePassword(
                            currentPassword,
                            newPassword
                        )
                    },
                    enabled =
                        currentPassword.isNotBlank() &&
                                newPassword.length >= 8 &&
                                confirmPassword == newPassword &&
                                !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {

                    Text("Update password")
                }
            }

            else {

                Button(
                    onClick = {
                        onForgotPassword(email)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text("Send recovery code")
                }

                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = recoveryCode,
                    onValueChange = {
                        recoveryCode = it
                    },
                    placeholder = {
                        Text("Recovery code")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                    },
                    placeholder = {
                        Text("New password")
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {

                        onResetPassword(
                            email,
                            recoveryCode,
                            newPassword
                        )
                    },
                    enabled =
                        recoveryCode.isNotBlank() &&
                                newPassword.length >= 8 &&
                                !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {

                    Text("Reset password")
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

private fun initials(name: String): String {

    return name
        .split(" ", "@", ".", "_", "-")
        .filter {
            it.isNotBlank()
        }
        .take(1)
        .mapNotNull {
            it.firstOrNull()?.uppercaseChar()?.toString()
        }
        .joinToString("")
        .ifBlank {
            "U"
        }
}