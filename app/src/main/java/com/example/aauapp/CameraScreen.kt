package com.example.aauapp

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.aauapp.BuildConfig
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScreen(
    facilityId: String = "aau",
    onAskDirections: (String) -> Unit = {},
    onScanRoom: () -> Unit = {},
    onScanQr: () -> Unit = {}
) {
    var showLiveCamera by remember { mutableStateOf(false) }

    if (!showLiveCamera) {
        CameraLandingScreen(
            onOpenCamera = { showLiveCamera = true },
            onScanRoom = onScanRoom,
            onScanQr = onScanQr
        )
    } else {
        LiveCameraScanner(
            facilityId = facilityId,
            onBack = { showLiveCamera = false },
            onAskDirections = onAskDirections
        )
    }
}

@Composable
private fun CameraLandingScreen(
    onOpenCamera: () -> Unit,
    onScanRoom: () -> Unit,
    onScanQr: () -> Unit
) {
    val pageBg = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surface
    val title = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(72.dp))

        Text(
            text = "Camera",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(22.dp))

        Text(
            text = "Scan rooms, signs and QR codes",
            style = MaterialTheme.typography.titleMedium,
            color = muted
        )

        Spacer(Modifier.height(22.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .background(Color(0xFFDCEBFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF2F6FEA),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "Ready to scan",
                    style = MaterialTheme.typography.headlineSmall,
                    color = title
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Point your camera at a sign, QR code or\nbuilding marker.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = muted
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        Text(
            text = "Actions",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF334155)
        )

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                CameraActionRow(
                    title = "Open camera",
                    subtitle = "Start the live detection feed.",
                    icon = Icons.Default.CameraAlt,
                    iconBg = Color(0xFF2563EB),
                    onClick = onOpenCamera
                )

                IosDivider()

                CameraActionRow(
                    title = "Scan Room",
                    subtitle = "Upload four photos (N/E/S/W) to summarize a room.",
                    icon = Icons.Default.Upload,
                    iconBg = Color(0xFF34A878),
                    onClick = onScanRoom
                )

                IosDivider()

                CameraActionRow(
                    title = "Scan QR Code",
                    subtitle = "Decode a building or location QR.",
                    icon = Icons.Default.QrCodeScanner,
                    iconBg = Color(0xFF8B4DE8),
                    onClick = onScanQr
                )
            }
        }
    }
}

@Composable
private fun CameraActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(iconBg, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.width(18.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF1F2937)
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF6B7280)
            )
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun IosDivider() {
    HorizontalDivider(
        color = Color(0xFFE5E7EB),
        thickness = 0.7.dp,
        modifier = Modifier.padding(start = 74.dp)
    )
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun LiveCameraScanner(
    facilityId: String,
    onBack: () -> Unit,
    onAskDirections: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = context as LifecycleOwner

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isConnected by remember { mutableStateOf(false) }
    var detections by remember { mutableStateOf<List<RemoteDetection>>(emptyList()) }
    var resolvedLocation by remember { mutableStateOf<RemoteLocation?>(null) }

    var showDirectionsDialog by remember { mutableStateOf(false) }
    var destinationQuery by remember { mutableStateOf("") }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasPermission = granted
        }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7)),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            ) {
                Text("Allow Camera")
            }
        }
        return
    }

    val visionService = remember {
        VisionStreamingService(
            onFrame = { frame ->
                detections = frame.detections
                resolvedLocation = frame.location
            },
            onConnected = { connected ->
                isConnected = connected
            }
        )
    }

    DisposableEffect(Unit) {
        visionService.connect(
            baseUrl = BuildConfig.BACKEND_BASE_URL,
            apiKey = BuildConfig.BACKEND_API_KEY,
            facilityId = facilityId
        )

        onDispose {
            visionService.disconnect()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                val executor = Executors.newSingleThreadExecutor()

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(executor) { imageProxy ->
                        visionService.sendFrame(imageProxy)
                        imageProxy.close()
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.08f))
        )

        DetectionOverlay(detections)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 46.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                containerColor = Color.Black.copy(alpha = 0.55f),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
            }

            Spacer(Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (isConnected) Color(0xFF00A651).copy(alpha = 0.85f)
                else Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    text = if (isConnected) "ML Vision Live" else "Connecting…",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        Button(
            onClick = {
                destinationQuery = resolvedLocation?.name
                    ?: detections.firstOrNull()?.label
                            ?: ""
                showDirectionsDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 24.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.65f),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Ask for Directions")
        }

        DetectionLabel(
            detections = detections,
            location = resolvedLocation,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 86.dp, start = 16.dp, end = 16.dp)
        )
    }

    if (showDirectionsDialog) {
        AlertDialog(
            onDismissRequest = { showDirectionsDialog = false },
            title = { Text("Where to?") },
            text = {
                OutlinedTextField(
                    value = destinationQuery,
                    onValueChange = { destinationQuery = it },
                    label = { Text("e.g. A101 or Cafeteria") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val query = destinationQuery.trim()
                        if (query.isNotBlank()) {
                            onAskDirections(query)
                        }
                        showDirectionsDialog = false
                    }
                ) {
                    Text("Get Directions")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectionsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetectionOverlay(detections: List<RemoteDetection>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        detections.forEach { detection ->
            val left = detection.x * size.width
            val top = detection.y * size.height
            val width = detection.width * size.width
            val height = detection.height * size.height

            drawRect(
                color = Color(0xFF00FF66),
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = 4f)
            )
        }
    }
}

@Composable
private fun DetectionLabel(
    detections: List<RemoteDetection>,
    location: RemoteLocation?,
    modifier: Modifier = Modifier
) {
    val label = location?.name
        ?: detections.firstOrNull()?.label
        ?: "Point camera at room or sign"

    val subtitle = location?.let {
        "${it.kind} • ${(it.confidence * 100).toInt()}%"
    } ?: if (detections.isNotEmpty()) {
        "${detections.size} detection(s)"
    } else {
        "Live ML Vision"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.Black.copy(alpha = 0.64f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}