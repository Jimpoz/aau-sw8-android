package com.example.aauapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            .padding(20.dp)
    ) {
        Text("Camera", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(20.dp))

        CameraCard("Open Camera", Icons.Outlined.CameraAlt, onOpenCamera)
        CameraCard("Scan Room", Icons.Outlined.Search, onScanRoom)
        CameraCard("Scan QR", Icons.Outlined.QrCodeScanner, onScanQr)
    }
}

@Composable
private fun CameraCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AndroidCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(modifier = Modifier.padding(18.dp)) {
            Icon(icon, contentDescription = null, tint = Blue600)
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}
