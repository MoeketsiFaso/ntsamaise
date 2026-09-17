package com.moeketsifaso.ntsamaise

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector

/** Create, use and close on the camera executor. CameraX drops stale frames. */
internal class SurroundingsDetector(context: Context) : AutoCloseable {
    private val detector = ObjectDetector.createFromOptions(
        context,
        ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("efficientdet_lite0.tflite")
                    .build()
            )
            // Synchronous inference on our worker keeps image ownership explicit.
            .setRunningMode(RunningMode.IMAGE)
            .setScoreThreshold(0.5f)
            .setMaxResults(5)
            .build()
    )

    fun detect(frame: ImageProxy): DetectionFeedback {
        val bitmap = frame.toBitmap()
        var upright = bitmap
        try {
            if (frame.imageInfo.rotationDegrees != 0) {
                upright = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height,
                    Matrix().apply { postRotate(frame.imageInfo.rotationDegrees.toFloat()) },
                    true
                )
            }
            val image = BitmapImageBuilder(upright).build()
            try {
                val category = detector.detect(image).detections()
                    .flatMap { it.categories() }
                    .filter { it.categoryName().isNotBlank() }
                    .maxByOrNull { it.score() }
                return if (category == null) {
                    DetectionFeedback("No object recognized", "No object recognized")
                } else {
                    val name = category.categoryName()
                    DetectionFeedback(
                        "$name ${(category.score() * 100).toInt()}%",
                        "$name detected"
                    )
                }
            } finally {
                image.close()
            }
        } finally {
            if (upright !== bitmap) upright.recycle()
            bitmap.recycle()
        }
    }

    override fun close() = detector.close()
}

internal data class DetectionFeedback(val display: String, val speech: String)
