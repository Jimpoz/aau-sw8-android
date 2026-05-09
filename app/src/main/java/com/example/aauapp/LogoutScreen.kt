package com.example.aauapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LogoutScreen(
    viewModel: UserSessionViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.profile

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Logout",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "This device is currently holding the signed-in user locally.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = user.displayName.ifBlank { "No user loaded" },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = user.email.ifBlank { "No email available" },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Button(
            onClick = viewModel::logout,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Log out")
        }
    }
}