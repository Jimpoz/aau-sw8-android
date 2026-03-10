package com.example.aauapp

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun FloorPlanScreen(){
    val viewModel: FloorPlanViewModel = viewModel()
    val userRoom by viewModel.userRoom.collectAsState()
    var userLocation by remember { mutableStateOf<Offset?>(null) }

    userLocation = getRoomCoordinates(userRoom)


    val points by viewModel.points.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            viewModel.attachPhoto("fake_uri_for_demo")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    viewModel.addPoint(offset)
                    cameraLauncher.launch(null)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            if (points.size > 1) {
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = Color.Blue,
                        start = androidx.compose.ui.geometry.Offset(
                            points[i].x,
                            points[i].y
                        ),
                        end = androidx.compose.ui.geometry.Offset(
                            points[i + 1].x,
                            points[i + 1].y
                        ),
                        strokeWidth = 5f
                    )
                }
            }

            points.forEach { point ->
                drawCircle(
                    color = if (point.photoUri != null)
                        Color.Green
                    else
                        Color.Red,
                    radius = 20f,
                    center = androidx.compose.ui.geometry.Offset(
                        point.x,
                        point.y
                    )
                )
            }
            userLocation?.let {
                drawCircle(
                    color = Color.Green,
                    radius = 30f,
                    center = it
                )
            }
        }
    }
}
fun getRoomCoordinates(room: String): Offset {
    return when(room) {
        "Room A" -> Offset(200f, 200f)
        "Lab 1" -> Offset(400f, 300f)
        "Hallway" -> Offset(600f, 500f)
        else -> Offset(100f, 100f)
    }
}
