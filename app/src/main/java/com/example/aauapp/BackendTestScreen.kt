package com.example.aauapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun BackendTestScreen(
    viewModel: BackendTestViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { viewModel.testConnection() }) {
            Text("Test Health")
        }

        Button(
            onClick = { viewModel.testCampuses() },
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Test Campuses")
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }

        if (uiState.result.isNotBlank()) {
            Text(
                text = uiState.result,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        uiState.error?.let {
            Text(
                text = "Error: $it",
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}