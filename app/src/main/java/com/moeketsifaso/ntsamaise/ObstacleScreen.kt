package com.moeketsifaso.ntsamaise

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun ObstacleScreen(onBack: () -> Unit) {
    val language = LocalAppLanguage.current
    val context = LocalContext.current.applicationContext
    var paused by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<DetectionFeedback?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastFrameTime by remember { mutableLongStateOf(0L) }
    var stale by remember { mutableStateOf(false) }
    var knownDistance by remember { mutableStateOf("1.0") }
    var calibration by remember { mutableStateOf<DistanceCalibration?>(null) }
    var stableObservation by remember { mutableStateOf<ObstacleObservation?>(null) }
    var distanceMeters by remember { mutableStateOf<Float?>(null) }
    val observationWindow = remember { DistanceObservationWindow() }
    val knownMeters = knownDistance.replace(',', '.').toFloatOrNull()
        ?.takeIf { it.isFinite() && it in 0.3f..10f }
    fun resetDistance() {
        observationWindow.clear()
        stableObservation = null
        calibration = null
        distanceMeters = null
    }
    val prompt = if (paused || stale || error != null) null else
        feedback?.speech?.takeIf { it.isNotBlank() }?.let { direction ->
            distanceMeters?.let { distanceSpeech(direction, it) } ?: direction
        }
    val speechError = rememberSpokenDetection(prompt, 400, 1_500, 5_000)
    var translation by remember { mutableStateOf<TranslationResult?>(null) }

    LaunchedEffect(prompt) {
        translation = prompt?.let { language.output(it) }
    }
    // Silence old results if inference stops producing frames, including after resume.
    LaunchedEffect(paused) {
        while (!paused) {
            delay(500)
            if (lastFrameTime != 0L && SystemClock.elapsedRealtime() - lastFrameTime > 2_500) {
                stale = true
                resetDistance()
            }
        }
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UiText("Detect Obstacles", style = MaterialTheme.typography.headlineMedium)
        UiText("Experimental camera alerts. Hold the phone upright, facing forward. " +
            "Left and right refer to the camera view. Calibrated distances are approximate. " +
            "Steps, potholes and other hazards may be missed. Keep using your usual mobility aid.")
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            UiText("Back to home")
        }
        Button(onClick = {
            paused = !paused
            feedback = null
            error = null
            lastFrameTime = 0L
            stale = false
            resetDistance()
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            UiText(if (paused) "Resume detection" else "Pause detection")
        }
        UiText(when {
            paused -> "Detection paused"
            error != null -> error!!
            stale -> "No recent camera results. Pause and resume detection to retry."
            else -> if (language == AppLanguage.ENGLISH) feedback?.display ?: "Looking for possible obstacles..."
                else if (prompt != null) translation?.takeIf { it.originalText == prompt }?.translatedText ?: "Translating to Sesotho..."
                else feedback?.display ?: "Looking for possible obstacles..."
        }, style = MaterialTheme.typography.titleLarge)
        UiText(if (distanceMeters != null && !paused && !stale && error == null)
            "${language.ui("Estimated distance")}: ${distanceText(distanceMeters!!)} m"
        else "Distance unavailable — calibrate and hold the same whole object steady.",
            style = MaterialTheme.typography.titleLarge)
        speechError?.let { UiText(it) }
        UiText("Distance calibration", style = MaterialTheme.typography.titleMedium)
        UiText("With a helper, measure from the camera to one stationary object (0.3–10 metres). " +
            "Show the whole object, hold steady, enter that distance and tap Calibrate. " +
            "Estimates apply only to this same object at the same angle. Do not switch objects or zoom. " +
            "Calibration clears when detection stops, the camera rotates, or the selected object class changes.")
        OutlinedTextField(
            value = knownDistance,
            onValueChange = { knownDistance = it },
            label = { UiText("Measured reference distance (metres)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = knownMeters == null,
            supportingText = { UiText("Enter a measured distance from 0.3 to 10 metres.") },
            modifier = Modifier.fillMaxWidth()
        )
        UiText(calibration?.let { "${language.ui("Calibrated for")}: ${distanceText(it.meters)} m" }
            ?: stableObservation?.let { language.ui("Ready to calibrate") }
            ?: "Waiting for a steady, fully visible object with sufficient confidence.")
        Button(onClick = {
            val reference = stableObservation
            val meters = knownMeters
            if (reference != null && meters != null) {
                calibration = DistanceCalibration(reference, meters)
                distanceMeters = meters
            }
        }, enabled = knownMeters != null && stableObservation != null && !paused && !stale && error == null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            UiText("Calibrate distance")
        }
        if (calibration != null) {
            Button(onClick = { resetDistance() }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                UiText("Clear calibration")
            }
        }
        if (!paused) {
            CameraPreview(
                createProcessor = { SurroundingsDetector(context, obstacles = true) },
                onResult = {
                    feedback = it
                    error = null
                    stale = false
                    lastFrameTime = SystemClock.elapsedRealtime()
                    val observation = it.obstacle
                    calibration?.let { saved ->
                        if (observation != null && (saved.reference.geometry != observation.geometry ||
                            saved.reference.bounds.label != observation.bounds.label)) resetDistance()
                    }
                    stableObservation = observationWindow.add(observation, lastFrameTime)
                    distanceMeters = calibration?.let { saved ->
                        stableObservation?.let { current -> estimateDistance(saved, current) }
                    }
                },
                onError = { error = it; feedback = null; resetDistance() },
                onReady = { ready ->
                    if (!ready) {
                        feedback = null
                        lastFrameTime = 0L
                        stale = false
                        resetDistance()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(280.dp)
            )
        }
    }
}
