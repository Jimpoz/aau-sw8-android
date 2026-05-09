package com.example.aauapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    viewModel: UserSessionViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    var isRegisterMode by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (isRegisterMode) "Create account" else "Log in",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = if (isRegisterMode) {
                        "Create an account to enter the app."
                    } else {
                        "Log in to open the main screen."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Button(
                    onClick = {
                        if (isRegisterMode) {
                            viewModel.register(name, email, password)
                        } else {
                            Text(
                                text = if (isSignupMode) "Create Account" else "Sign In",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(if (isRegisterMode) "Create account" else "Log in")
                }
            }

                TextButton(
                    onClick = { isRegisterMode = !isRegisterMode },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        if (isRegisterMode) {
                            "Already have an account?"
                        } else {
                            "Need to create an account?"
                        }
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
                    color = AndroidCard.copy(alpha = 0.95f)
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
                    containerColor = AndroidCard.copy(alpha = 0.18f),
                    contentColor = AndroidCard
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
                color = AndroidCard.copy(alpha = 0.8f)
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
}

@Composable
private fun Header(isSignupMode: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AndroidCard.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                tint = AndroidCard,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Ariadne",
            style = MaterialTheme.typography.headlineLarge,
            color = AndroidCard
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isSignupMode) "Create your account" else "Sign in to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = AndroidCard.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun ModeToggle(
    isSignupMode: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AndroidCard.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AndroidCard.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
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
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) AndroidCard else AndroidCard.copy(alpha = 0.0f),
            contentColor = if (selected) Blue700 else AndroidCard
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(text)
    }
}
