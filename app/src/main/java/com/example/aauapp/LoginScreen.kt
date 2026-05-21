package com.example.aauapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.aauapp.ui.theme.Blue500
import com.example.aauapp.ui.theme.Blue700

@Composable
fun LoginScreen(
    viewModel: UserSessionViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    var isSignupMode by remember { mutableStateOf(false) }
    var isMemberSignup by remember { mutableStateOf(true) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var organizationId by remember { mutableStateOf("") }
    var showForgot by remember { mutableStateOf(false) }

    val canSubmit =
        email.isNotBlank() &&
                password.isNotBlank() &&
                (!isSignupMode || password.length >= 8) &&
                (!isSignupMode || !isMemberSignup || organizationId.isNotBlank())

    val white = MaterialTheme.colorScheme.onPrimary
    val glass = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Blue500, Blue700)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(isSignupMode = isSignupMode)

            Spacer(modifier = Modifier.height(24.dp))

            ModeToggle(
                isSignupMode = isSignupMode,
                onChange = { isSignupMode = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = glass
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isSignupMode) {
                        SignupKindToggle(
                            isMemberSignup = isMemberSignup,
                            onChange = { isMemberSignup = it }
                        )

                        Text(
                            text = if (isMemberSignup) {
                                "Use your organization ID to join your campus, company, or institution."
                            } else {
                                "Sign up without joining an organization. You'll see public places."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = white.copy(alpha = 0.85f)
                        )
                    }

                    LoginField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email"
                    )

                    LoginField(
                        value = password,
                        onValueChange = { password = it },
                        label = if (isSignupMode) "Password (min 8 characters)" else "Password",
                        isPassword = true
                    )

                    if (isSignupMode) {
                        LoginField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = "Full name (optional)"
                        )
                    }

                    if (!isSignupMode || isMemberSignup) {
                        LoginField(
                            value = organizationId,
                            onValueChange = { organizationId = it },
                            label = if (isSignupMode) "Organization ID" else "Organization ID (optional)"
                        )
                    }

                    if (uiState.error != null && uiState.mfaChallengeToken == null) {
                        Text(
                            text = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (isSignupMode) {
                                viewModel.signup(
                                    email = email,
                                    password = password,
                                    fullName = fullName,
                                    organizationId = if (isMemberSignup) organizationId else null
                                )
                            } else {
                                viewModel.login(
                                    email = email,
                                    password = password,
                                    organizationId = organizationId.ifBlank { null }
                                )
                            }
                        },
                        enabled = canSubmit && !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = Blue700
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Blue700,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isSignupMode) "Create Account" else "Sign In",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            if (!isSignupMode) {
                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = { showForgot = true }) {
                    Text(
                        text = "Forgot password?",
                        color = white.copy(alpha = 0.95f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = { isSignupMode = !isSignupMode }
            ) {
                Text(
                    text = if (isSignupMode) {
                        "Already have an account? Sign in"
                    } else {
                        "Don't have an account? Sign up"
                    },
                    color = white.copy(alpha = 0.95f)
                )
            }

            GuestDivider()

            Button(
                onClick = { viewModel.guestLogin() },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = glass,
                    contentColor = white
                )
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue as guest")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Browse public places without creating an account.",
                style = MaterialTheme.typography.bodySmall,
                color = white.copy(alpha = 0.8f)
            )
        }
    }

    if (uiState.mfaChallengeToken != null) {
        MfaCodeDialog(
            isLoading = uiState.isLoading,
            error = uiState.error,
            onVerify = { code ->
                viewModel.completeMfaLogin(code)
            }
        )
    }

    if (showForgot) {
        ForgotPasswordDialog(
            initialEmail = email,
            isLoading = uiState.isLoading,
            message = uiState.message,
            error = uiState.error,
            onSendCode = { viewModel.forgotPassword(it) },
            onReset = { mail, code, newPassword ->
                viewModel.resetPassword(mail, code, newPassword)
            },
            onDismiss = { showForgot = false }
        )
    }
}

@Composable
private fun ForgotPasswordDialog(
    initialEmail: String,
    isLoading: Boolean,
    message: String?,
    error: String?,
    onSendCode: (String) -> Unit,
    onReset: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var mail by remember { mutableStateOf(initialEmail) }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (codeSent) "Reset password" else "Forgot password") },
        text = {
            Column {
                OutlinedTextField(
                    value = mail,
                    onValueChange = { mail = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (codeSent) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("6-digit code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New password (min 8)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (!codeSent) {
                Button(
                    onClick = {
                        onSendCode(mail.trim())
                        codeSent = true
                    },
                    enabled = mail.isNotBlank() && !isLoading
                ) { Text("Send code") }
            } else {
                Button(
                    onClick = { onReset(mail.trim(), code.trim(), newPassword) },
                    enabled = code.isNotBlank() && newPassword.length >= 8 && !isLoading
                ) { Text("Reset") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun Header(isSignupMode: Boolean) {
    val white = MaterialTheme.colorScheme.onPrimary

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(white.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                tint = white,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Ariadne",
            style = MaterialTheme.typography.headlineLarge,
            color = white
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isSignupMode) "Create your account" else "Sign in to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = white.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun ModeToggle(
    isSignupMode: Boolean,
    onChange: (Boolean) -> Unit
) {
    val white = MaterialTheme.colorScheme.onPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(white.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        ToggleButton(
            text = "Sign In",
            selected = !isSignupMode,
            modifier = Modifier.weight(1f),
            onClick = { onChange(false) }
        )

        ToggleButton(
            text = "Sign Up",
            selected = isSignupMode,
            modifier = Modifier.weight(1f),
            onClick = { onChange(true) }
        )
    }
}

@Composable
private fun SignupKindToggle(
    isMemberSignup: Boolean,
    onChange: (Boolean) -> Unit
) {
    val white = MaterialTheme.colorScheme.onPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(white.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        ToggleButton(
            text = "Member",
            selected = isMemberSignup,
            modifier = Modifier.weight(1f),
            onClick = { onChange(true) }
        )

        ToggleButton(
            text = "Personal",
            selected = !isMemberSignup,
            modifier = Modifier.weight(1f),
            onClick = { onChange(false) }
        )
    }
}

@Composable
private fun ToggleButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val white = MaterialTheme.colorScheme.onPrimary

    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.surface
            } else {
                white.copy(alpha = 0.0f)
            },
            contentColor = if (selected) {
                Blue700
            } else {
                white
            }
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label) },
        singleLine = true,
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.surface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun GuestDivider() {
    val white = MaterialTheme.colorScheme.onPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = white.copy(alpha = 0.35f)
        )

        Text(
            text = "or",
            color = white.copy(alpha = 0.85f),
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = white.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun MfaCodeDialog(
    isLoading: Boolean,
    error: String?,
    onVerify: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text("Enter sign-in code")
        },
        text = {
            Column {
                Text(
                    text = "Check your email or authenticator app and enter the code.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerify(code) },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text("Verify")
                }
            }
        }
    )
}