package com.example.aauapp

import android.graphics.Bitmap
import android.util.Log
import android.util.Log.e
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class LocationDetector {

    private val labeler =
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    fun detectLocation(bitmap: Bitmap, onLocationDetected: (String) -> Unit) {

        val image = InputImage.fromBitmap(bitmap, 0)

        labeler.process(image)
            .addOnSuccessListener { labels ->

                for (label in labels) {

                    val text = label.text
                    val confidence = label.confidence

                    Log.d("ML KIT", "Detected: $text $confidence")

                    when (text) {

                        "Poster" -> onLocationDetected("Room A")

                        "Whiteboard" -> onLocationDetected("Lab 1")

                        "Door" -> onLocationDetected("Hallway")

                    }
                }
            }
            .addOnFailureListener {
                e("ML KIT", "Detection failed")
            }
    }
}