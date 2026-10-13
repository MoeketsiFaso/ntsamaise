package com.moeketsifaso.ntsamaise

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CameraScreen() {
    val language = LocalAppLanguage.current
    val context = LocalContext.current.applicationContext
    var detectedObject by remember { mutableStateOf("Looking for objects...") }
    var displayedSesotho by remember { mutableStateOf<String?>(null) }
    var translationUnavailable by remember { mutableStateOf(false) }
    var spokenResult by remember { mutableStateOf<String?>(null) }
    var lastFrameAt by remember { mutableStateOf<Long?>(null) }
    val speechError = rememberSpokenDetection(spokenResult)

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            if (lastFrameAt?.let { SystemClock.elapsedRealtime() - it > 2_500 } == true) {
                spokenResult = null
                displayedSesotho = null
                detectedObject = "Waiting for camera..."
                lastFrameAt = null
            }
        }
    }

    LaunchedEffect(language, spokenResult) {
        if (spokenResult != null) {
            val translation = language.output(spokenResult!!)
            displayedSesotho = translation.translatedText
            translationUnavailable = translation.source == TranslationSource.FALLBACK
        } else {
            displayedSesotho = null
            translationUnavailable = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            createProcessor = { SurroundingsDetector(context) },
            onResult = { feedback ->
                lastFrameAt = SystemClock.elapsedRealtime()
                if (spokenResult != feedback.speech) displayedSesotho = null
                detectedObject = feedback.display
                spokenResult = feedback.speech
            },
            onError = { message ->
                lastFrameAt = null
                displayedSesotho = null
                detectedObject = message
                spokenResult = null
            },
            onReady = { ready -> if (!ready) {
                lastFrameAt = null
                spokenResult = null
                displayedSesotho = null
                detectedObject = "Looking for objects..."
            } },
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)
                .background(Color.Black.copy(alpha = 0.65f)).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UiText(
                text = if (language == AppLanguage.ENGLISH) detectedObject else displayedSesotho ?: language.ui(if (spokenResult != null) "Translating to Sesotho..." else detectedObject),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (language == AppLanguage.SESOTHO && displayedSesotho != null) {
                Regex("\\b(\\d+)%").find(detectedObject)?.groupValues?.get(1)?.let {
                    Text("${language.ui("Object confidence")}: $it%", color = Color.LightGray)
                }
                val confidence = when {
                    detectedObject.endsWith("low colour confidence") -> "Low"
                    detectedObject.endsWith("moderate colour confidence") -> "Moderate"
                    else -> null
                }
                confidence?.let {
                    Text("${language.ui("Colour confidence")}: ${language.ui(it)}", color = Color.LightGray)
                }
                if (translationUnavailable) UiText("Sesotho translation unavailable. Showing original text.", color = Color.White)
            }

        }
        speechError?.let { message ->
            UiText(
                text = message,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).padding(24.dp)
                    .background(Color.Black.copy(alpha = 0.85f)).padding(12.dp)
            )
        }
    }
}
