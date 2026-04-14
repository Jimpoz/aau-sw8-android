package com.example.aauapp

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

data class ImagePrediction(
    val label: String,
    val score: Float
)

class MobileNetClassifier(context: Context) {

    private val classifier: ImageClassifier

    init {
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setMaxResults(3)
            .setScoreThreshold(0.1f)
            .build()

        classifier = ImageClassifier.createFromFileAndOptions(
            context,
            "mobilenet.tflite",
            options
        )
    }

    fun classify(bitmap: Bitmap): List<ImagePrediction> {
        val safeBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val image = TensorImage.fromBitmap(safeBitmap)
        val results = classifier.classify(image)

        return results.flatMap { classifications ->
            classifications.categories.map { category ->
                ImagePrediction(
                    label = category.label,
                    score = category.score
                )
            }
        }.sortedByDescending { it.score }
    }
}