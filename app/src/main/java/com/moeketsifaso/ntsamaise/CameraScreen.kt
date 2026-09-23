package com.moeketsifaso.ntsamaise

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

@Composable
fun CameraScreen() {
    val context = LocalContext.current.applicationContext
    var detectedObject by remember { mutableStateOf("Looking for objects...") }
    var displayedSesotho by remember { mutableStateOf<String?>("E batla lintho...") }
    var spokenResult by remember { mutableStateOf<String?>(null) }
    val speechError = rememberSpokenDetection(spokenResult)

    LaunchedEffect(spokenResult) {
        if (spokenResult != null) {
            val translation = GoogleCloudTranslator.translate(spokenResult!!)
            displayedSesotho = translation.translatedText
        } else {
            displayedSesotho = if (detectedObject == "Looking for objects...") "E batla lintho..." else null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            createProcessor = { SurroundingsDetector(context) },
            onResult = { feedback ->
                detectedObject = feedback.display
                spokenResult = feedback.speech
            },
            onError = { message ->
                detectedObject = message
                spokenResult = null
            },
            onReady = { ready -> if (!ready) spokenResult = null },
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)
                .background(Color.Black.copy(alpha = 0.65f)).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = displayedSesotho ?: detectedObject,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (displayedSesotho != null && displayedSesotho != detectedObject) {
                Text(
                    text = detectedObject,
                    fontSize = 16.sp,
                    color = Color.LightGray
                )
            }
        }
        speechError?.let { message ->
            Text(
                text = message,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).padding(24.dp)
                    .background(Color.Black.copy(alpha = 0.85f)).padding(12.dp)
            )
        }
    }
}
