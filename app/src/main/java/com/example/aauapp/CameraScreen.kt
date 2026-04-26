package com.example.aauapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.aauapp.ui.theme.*

@Composable
fun CameraScreen(
    onOpenCamera: () -> Unit = {},
    onScanRoom: () -> Unit = {},
    onScanQr: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Camera",
            style = MaterialTheme.typography.headlineLarge,
            color = Slate900
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Scan rooms, signs and QR codes.",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate500
        )

        Spacer(modifier = Modifier.height(18.dp))

        CameraHeroCard(onClick = onOpenCamera)

        Spacer(modifier = Modifier.height(18.dp))

        SectionTitle("Actions")

        CameraActionCard(
            title = "Open Camera",
            subtitle = "Open the live camera scanner.",
            icon = Icons.Default.PhotoCamera,
            selected = true,
            onClick = onOpenCamera
        )

        CameraActionCard(
            title = "Scan Room",
            subtitle = "Upload room photos to the backend.",
            icon = Icons.Default.Upload,
            onClick = onScanRoom
        )

        CameraActionCard(
            title = "Scan QR Code",
            subtitle = "Read a room or map QR code.",
            icon = Icons.Default.QrCodeScanner,
            onClick = onScanQr
        )

        CameraActionCard(
            title = "Find Location",
            subtitle = "Match camera results with the floor plan.",
            icon = Icons.Default.Search,
            onClick = onOpenCamera
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun CameraHeroCard(
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Blue600),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AndroidCard.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = null,
                    tint = AndroidCard,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Ready to scan",
                style = MaterialTheme.typography.headlineMedium,
                color = AndroidCard
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Point your camera at a room sign, QR code or building marker.",
                style = MaterialTheme.typography.bodyMedium,
                color = AndroidCard.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Slate400,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun CameraActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Blue50 else AndroidCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (selected) Blue600 else Blue50),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) AndroidCard else Blue600
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate900
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )
            }
        }
    }
}