package com.moeketsifaso.ntsamaise

import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReadTextScreen(onBack: () -> Unit) = CapturedTextScreen(onBack, currencyMode = false)

@Composable
fun CurrencyScreen(onBack: () -> Unit) = CapturedTextScreen(onBack, currencyMode = true)

/** Shared capture, cancellation and speech controls for passages and banknote text. */
@Composable
private fun CapturedTextScreen(onBack: () -> Unit, currencyMode: Boolean) {
    val captureLabel = if (currencyMode) "Scan banknote" else "Capture and read"
    val initialGuidance = if (currencyMode) {
        "Place one Lesotho note flat. Show the whole side with the bank name, then tap Scan banknote."
    } else "Point the camera at printed text, then tap Capture and read."
    val requests = remember { TextCaptureRequests() }
    val speech = rememberSpeechOutput()
    var cameraReady by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var recognizedText by rememberSaveable { mutableStateOf("") }
    var status by remember {
        mutableStateOf(if (recognizedText.isBlank()) {
            initialGuidance
        } else if (currencyMode) recognizedText else "Text captured. Tap Read again to read it aloud.")
    }
    var pendingSpeech by remember { mutableStateOf<String?>(null) }

    // Consume each passage once, including when TTS initializes after OCR finishes.
    LaunchedEffect(pendingSpeech, speech.ready, speech.resumed) {
        if (!speech.resumed) {
            pendingSpeech = null
        } else if (speech.ready) {
            pendingSpeech?.let {
                pendingSpeech = null
                speech.speak(it)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 56.dp)) {
            Text("Back to home")
        }
        Text(if (currencyMode) "Identify Money" else "Read Text", style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() })
        if (currencyMode) {
            Text("Text-based prototype for 10, 20, 50, 100 and 200 maloti notes. Coins and other currencies are not supported. It cannot check whether a note is genuine.")
        }
        CameraPreview(
            createProcessor = { PrintedTextRecognizer(requests) },
            imageFormat = ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888,
            resolution = Size(1280, 960),
            modifier = Modifier.fillMaxWidth().height(220.dp),
            onReady = { ready ->
                cameraReady = ready
                if (!ready) {
                    requests.cancel()
                    pendingSpeech = null
                    if (scanning) status = "Capture cancelled. Tap $captureLabel to try again."
                    scanning = false
                }
            },
            onResult = { result ->
                if (requests.complete(result.requestId)) {
                    scanning = false
                    recognizedText = when {
                        result.error != null -> ""
                        currencyMode -> LotiCurrencyReader.read(result.text).message
                        else -> result.text
                    }
                    status = result.error ?: if (currencyMode) {
                        recognizedText
                    } else if (result.text.isBlank()) {
                        "No text found. Improve the lighting, move closer and try again."
                    } else {
                        "Text captured. Tap Read again to repeat, or capture another passage."
                    }
                    pendingSpeech = recognizedText.ifBlank { status }
                }
            },
            onError = { message ->
                requests.cancel()
                scanning = false
                status = message
                pendingSpeech = message
            }
        )
        Text(status, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Button(
            onClick = {
                if (requests.request() != null) {
                    speech.stop()
                    pendingSpeech = null
                    recognizedText = ""
                    scanning = true
                    status = "Recognizing text. Hold the camera steady."
                }
            },
            enabled = cameraReady && !scanning,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) { Text(if (scanning) "Recognizing text…" else captureLabel) }
        OutlinedButton(
            onClick = { pendingSpeech = recognizedText },
            enabled = recognizedText.isNotBlank() && speech.ready && !scanning,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) { Text(if (currencyMode) "Repeat result" else "Read again") }
        OutlinedButton(
            onClick = {
                pendingSpeech = null
                speech.stop()
                requests.cancel()
                if (scanning) status = "Capture cancelled. Tap $captureLabel to try again."
                scanning = false
            },
            enabled = speech.isSpeaking || pendingSpeech != null || scanning,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) { Text(if (scanning) "Cancel capture" else "Stop reading") }
        speech.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!currencyMode && recognizedText.isNotBlank()) {
            Text("Recognized text", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            SelectionContainer { Text(recognizedText, fontSize = 20.sp) }
        }
        Text(if (currencyMode) "Results are suggestions from printed text. Reading aloud uses an English voice."
            else "Printed Latin-script text. Reading aloud currently uses an English voice.")
    }
}
