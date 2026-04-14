package com.example.aauapp

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class LocationDetector(context: Context) {

    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    fun detectLocation(bitmap: Bitmap, onLocationDetected: (String) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                val best = labels.maxByOrNull { it.confidence }

                val result = when (best?.text?.lowercase()) {
                    "whiteboard" -> "Lab"
                    "poster" -> "Room A"
                    "door" -> "Hallway"
                    "screen", "monitor" -> "Study Area"
                    else -> "Unknown indoor area"
                }

                onLocationDetected(result)
            }
            .addOnFailureListener {
                onLocationDetected("Detection failed")
            }
    }
}