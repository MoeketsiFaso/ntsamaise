package com.moeketsifaso.ntsamaise

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

internal data class TextCaptureResult(
    val requestId: Long,
    val text: String,
    val error: String? = null
)

/** The bundled Latin-script model works without a first-run download. */
internal class PrintedTextRecognizer(private val requests: TextCaptureRequests) :
    CameraFrameProcessor<TextCaptureResult> {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @OptIn(ExperimentalGetImage::class)
    override fun process(frame: ImageProxy): TextCaptureResult? {
        val mediaImage = frame.image ?: return null
        val requestId = requests.take() ?: return null
        return try {
            val input = InputImage.fromMediaImage(mediaImage, frame.imageInfo.rotationDegrees)
            TextCaptureResult(requestId, recognize(input))
        } catch (_: Exception) {
            // Keep the request ID even on failure so cancelled requests cannot update the UI.
            TextCaptureResult(requestId, "", "Could not read this image. Hold the camera steady and try again.")
        }
    }

    internal fun recognize(input: InputImage): String {
        // The worker keeps the frame open until OCR completes; never call on the main thread.
        return Tasks.await(recognizer.process(input)).text.trim()
    }

    override fun close() = recognizer.close()
}
