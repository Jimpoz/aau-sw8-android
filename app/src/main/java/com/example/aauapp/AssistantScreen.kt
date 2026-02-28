package com.example.aauapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AssistantScreen(viewModel: AssistantViewModel = viewModel()) {

    var text by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<String>() }

    // Collect messages from ViewModel
    LaunchedEffect(viewModel.messages) {
        viewModel.messages.collectLatest {
            messages.clear()
            messages.addAll(it)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { msg ->
                Text(
                    text = msg,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
        ) {

            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                if (text.isNotEmpty()) {
                    viewModel.sendMessage(text)
                    text = ""
                }
            }) {
                Text("Send")
            }
        }
    }
}