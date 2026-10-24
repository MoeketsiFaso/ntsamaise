package com.moeketsifaso.ntsamaise

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import android.graphics.RectF
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.Button

@Composable
fun CurrencyScreen(onBack: () -> Unit) {
    val language = LocalAppLanguage.current
    val context = LocalContext.current.applicationContext

    var displayedResult by remember { mutableStateOf("Point camera at a banknote, then capture.") }
    var displayedSesotho by remember { mutableStateOf<String?>(null) }
    var translationUnavailable by remember { mutableStateOf(false) }
    var spokenResult by remember { mutableStateOf<String?>(null) }
    var currentDetections by remember { mutableStateOf<List<BanknoteDetection>>(emptyList()) }
    var viewRects by remember { mutableStateOf<List<RectF>>(emptyList()) }
    var isCapturing by remember { mutableStateOf(false) }
    var captureAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val activity = LocalContext.current as? MainActivity

    rememberSpokenDetection(spokenResult)
    DisposableEffect(activity, captureAction) {
        activity?.setCurrencyCaptureAction(captureAction)
        onDispose { activity?.setCurrencyCaptureAction(null) }
    }

    // Resolve displayed translation in the selected language
    LaunchedEffect(language, spokenResult, displayedResult) {
        if (spokenResult != null) {
            val translation = language.output(displayedResult)
            displayedSesotho = translation.translatedText
            translationUnavailable = translation.source == TranslationSource.FALLBACK
        } else {
            displayedSesotho = null
            translationUnavailable = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.heightIn(min = 56.dp)
        ) {
            UiText("Back to home")
        }

        UiText(
            "Identify Money",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )

        UiText(
            "Vision model recognizes 10, 20, 50, 100 and 200 Maloti and Rand notes. Does not verify banknote authenticity.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Live camera preview with visual bounding box overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            CameraPreview(
                createProcessor = { YoloCurrencyDetector(context) },
                captureMode = true,
                captureEnabled = !isCapturing,
                onCaptureState = { capturing ->
                    isCapturing = capturing
                    if (capturing) {
                        displayedResult = "Analyzing captured image..."
                        displayedSesotho = null
                        spokenResult = null
                        translationUnavailable = false
                        currentDetections = emptyList()
                        viewRects = emptyList()
                    }
                },
                onCaptureAction = { captureAction = it },
                onResult = { feedback ->
                    if (spokenResult != feedback.speech) {
                        displayedSesotho = null
                    }
                    displayedResult = feedback.display
                    spokenResult = feedback.speech
                    currentDetections = feedback.detections
                    viewRects = feedback.previewBoxes
                },
                onError = { message ->
                    isCapturing = false
                    displayedSesotho = null
                    displayedResult = message
                    spokenResult = null
                    currentDetections = emptyList()
                    viewRects = emptyList()
                },
                onReady = { ready ->
                    if (!ready) {
                        displayedSesotho = null
                        if (!isCapturing) displayedResult = "Point camera at a banknote, then capture."
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Bounding box overlay for recognized notes
            if (currentDetections.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.dp.toPx()
                    for (box in viewRects) {
                        drawRect(
                            color = Color(0xFF00E676),
                            topLeft = Offset(box.left, box.top),
                            size = Size(box.width(), box.height()),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
            }
        }

        Button(
            onClick = { if (!isCapturing) captureAction?.invoke() },
            enabled = !isCapturing && captureAction != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
        ) {
            UiText(if (isCapturing) "Analyzing captured image..." else "Capture banknote (Volume Up)")
        }

        // Recognition output card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                UiText(
                    "Result",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                if (language == AppLanguage.SESOTHO) {
                    Text(
                        text = displayedSesotho ?: language.ui(displayedResult),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (translationUnavailable) {
                        UiText(
                            "Sesotho translation unavailable. Showing original text.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (displayedResult != displayedSesotho && displayedSesotho != null) {
                        Text(
                            text = displayedResult,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Text(
                        text = displayedResult,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
