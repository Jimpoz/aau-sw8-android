package com.example.aauapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.aauapp.ui.theme.*

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Assistant",
            style = MaterialTheme.typography.headlineLarge,
            color = Slate900
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Ask for directions, rooms and campus help.",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate500
        )

        Spacer(modifier = Modifier.height(16.dp))

        AssistantHeroCard()

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                AssistantBubble(message)
            }
        }

        AssistantInputBar(
            value = input,
            onValueChange = { input = it },
            onSend = {
                val clean = input.trim()
                if (clean.isNotEmpty()) {
                    viewModel.sendMessage(clean)
                    input = ""
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun AssistantHeroCard() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Blue600),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(AndroidCard.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AndroidCard
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Campus AI",
                    style = MaterialTheme.typography.titleLarge,
                    color = AndroidCard
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Connected to your backend assistant.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AndroidCard.copy(alpha = 0.82f)
                )
            }
        }
    }
}

@Composable
private fun AssistantBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = if (isUser) 22.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 22.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Blue600 else AndroidCard
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                color = if (isUser) AndroidCard else Slate900,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun AssistantInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = AndroidCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = "Ask anything...",
                        color = Slate400
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AndroidCard,
                    unfocusedContainerColor = AndroidCard,
                    disabledContainerColor = AndroidCard,
                    focusedIndicatorColor = AndroidCard,
                    unfocusedIndicatorColor = AndroidCard,
                    disabledIndicatorColor = AndroidCard
                )
            )

            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Blue600)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = AndroidCard
                )
            }
        }
    }
}