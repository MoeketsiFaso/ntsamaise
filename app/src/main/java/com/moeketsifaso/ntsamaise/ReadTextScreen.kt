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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReadTextScreen(onBack: () -> Unit) = CapturedTextScreen(onBack, currencyMode = false)

/** Shared capture, cancellation and speech controls for passages and banknote text. */
@Composable
private fun CapturedTextScreen(onBack: () -> Unit, currencyMode: Boolean) {
    val language = LocalAppLanguage.current
    var translationUnavailable by rememberSaveable { mutableStateOf(false) }
    val captureLabel = if (currencyMode) "Scan banknote" else "Capture and read"
    val initialGuidance = if (currencyMode) {
        "Place one Lesotho note flat. Show the whole side with the bank name, then tap Scan banknote."
    } else "Point the camera at printed text, then tap Capture and read."
    val requests = remember { TextCaptureRequests() }
    val speech = rememberSpeechOutput()
    var cameraReady by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
    var recognizedText by rememberSaveable { mutableStateOf("") }
    var translatedText by rememberSaveable { mutableStateOf("") }
    var status by remember {
        mutableStateOf(if (recognizedText.isBlank()) {
            initialGuidance
        } else if (currencyMode) {
            translatedText.ifBlank { recognizedText }
        } else "Text captured. Tap Read again to read it aloud.")
    }
    var pendingSpeech by remember { mutableStateOf<String?>(null) }

    // Resolve captured content in the selected output language.
    LaunchedEffect(language, recognizedText, isTranslating) {
        if (isTranslating && recognizedText.isNotBlank()) {
            val translation = language.output(recognizedText)
            translationUnavailable = translation.source == TranslationSource.FALLBACK
            translatedText = translation.translatedText
            isTranslating = false
            status = if (currencyMode) {
                translatedText
            } else {
                "Text captured. Tap Read again to read it aloud."
            }
            pendingSpeech = translatedText
        }
    }

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
            UiText("Back to home")
        }
        UiText(
            if (currencyMode) "Identify Money" else "Read Text",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )
        if (currencyMode) {
            UiText("Text-based prototype for 10, 20, 50, 100 and 200 maloti notes. Coins and other currencies are not supported. It cannot check whether a note is genuine.")
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
                    isTranslating = false
                    if (scanning) status = "Capture cancelled. Try again when ready."
                    scanning = false
                }
            },
            onResult = { result ->
                if (requests.complete(result.requestId)) {
                    scanning = false
                    val extractedText = when {
                        result.error != null -> ""
                        currencyMode -> LotiCurrencyReader.read(result.text).message
                        else -> result.text
                    }
                    recognizedText = extractedText
                    if (result.error != null) {
                        translatedText = ""
                        isTranslating = false
                        status = result.error
                        pendingSpeech = language.ui(result.error)
                    } else if (extractedText.isBlank()) {
                        val emptyMsg = if (currencyMode) {
                            "No text found. Hold one note flat in good light and scan again."
                        } else {
                            "No text found. Improve the lighting, move closer and try again."
                        }
                        translatedText = ""
                        isTranslating = false
                        status = emptyMsg
                        pendingSpeech = language.ui(emptyMsg)
                    } else {
                        translatedText = ""
                        isTranslating = true
                        status = if (language == AppLanguage.ENGLISH) "Recognizing text..." else "Translating to Sesotho..."
                    }
                }
            },
            onError = { message ->
                requests.cancel()
                scanning = false
                isTranslating = false
                status = message
                pendingSpeech = language.ui(message)
            }
        )
        UiText(status, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Button(
            onClick = {
                if (requests.request() != null) {
                    speech.stop()
                    pendingSpeech = null
                    recognizedText = ""
                    translatedText = ""
                    translationUnavailable = false
                    isTranslating = false
                    scanning = true
                    status = "Recognizing text. Hold the camera steady."
                }
            },
            enabled = cameraReady && !scanning && !isTranslating,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) { UiText(if (scanning) "Recognizing text…" else captureLabel) }
        OutlinedButton(
            onClick = { pendingSpeech = translatedText.ifBlank { recognizedText } },
            enabled = (translatedText.isNotBlank() || recognizedText.isNotBlank()) && speech.ready && !scanning && !isTranslating,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) { UiText(if (currencyMode) "Repeat result" else "Read again") }
        OutlinedButton(
            onClick = {
                pendingSpeech = null
                speech.stop()
                requests.cancel()
                isTranslating = false
                if (scanning) status = "Capture cancelled. Try again when ready."
                scanning = false
            },
            enabled = speech.isSpeaking || pendingSpeech != null || scanning || isTranslating,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) { UiText(if (scanning || isTranslating) "Cancel capture" else "Stop reading") }
        speech.error?.let { UiText(it, color = MaterialTheme.colorScheme.error) }
        if (translationUnavailable) UiText("Sesotho translation unavailable. Showing original text.")
        if (translatedText.isNotBlank()) {
            UiText(
                if (currencyMode) "Result" else "Recognized text",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            SelectionContainer {
                Text(translatedText, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }
        }
        if (!currencyMode && recognizedText.isNotBlank() && translatedText != recognizedText) {
            UiText(
                "Original text",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            SelectionContainer {
                Text(recognizedText, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        UiText(
            if (currencyMode) "Results are suggestions from printed text."
            else "Printed text is read in your selected language when translation is available."
        )
    }
}
