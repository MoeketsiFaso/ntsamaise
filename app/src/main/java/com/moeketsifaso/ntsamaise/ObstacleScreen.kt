package com.moeketsifaso.ntsamaise

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun ObstacleScreen(onBack: () -> Unit) {
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
        translation = prompt?.let { GoogleCloudTranslator.translate(it) }
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
        Text("Detect Obstacles", style = MaterialTheme.typography.headlineMedium)
        Text("Experimental camera alerts. Hold the phone upright, facing forward. " +
            "Left and right refer to the camera view. Calibrated distances are approximate. " +
            "Steps, potholes and other hazards may be missed. Keep using your usual mobility aid.")
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text("Back to home")
        }
        Button(onClick = {
            paused = !paused
            feedback = null
            error = null
            lastFrameTime = 0L
            stale = false
            resetDistance()
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(if (paused) "Resume detection" else "Pause detection")
        }
        Text(when {
            paused -> "Detection paused"
            error != null -> error!!
            stale -> "No recent camera results. Pause and resume detection to retry."
            else -> feedback?.display ?: "Looking for possible obstacles..."
        }, style = MaterialTheme.typography.titleLarge)
        Text(if (distanceMeters != null && !paused && !stale && error == null)
            "Estimated distance: ${distanceText(distanceMeters!!)} m"
        else "Distance unavailable — calibrate and hold the same whole object steady.",
            style = MaterialTheme.typography.titleLarge)
        translation?.takeIf { prompt != null && it.originalText == prompt }?.let {
            Text(it.translatedText, style = MaterialTheme.typography.titleLarge)
        }
        speechError?.let { Text(it) }
        Text("Distance calibration", style = MaterialTheme.typography.titleMedium)
        Text("With a helper, measure from the camera to one stationary object (0.3–10 metres). " +
            "Show the whole object, hold steady, enter that distance and tap Calibrate. " +
            "Estimates apply only to this same object at the same angle. Do not switch objects or zoom. " +
            "Calibration clears when detection stops, the camera rotates, or the selected object class changes.")
        OutlinedTextField(
            value = knownDistance,
            onValueChange = { knownDistance = it },
            label = { Text("Measured reference distance (metres)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = knownMeters == null,
            supportingText = { Text("Enter a measured distance from 0.3 to 10 metres.") },
            modifier = Modifier.fillMaxWidth()
        )
        Text(calibration?.let { "Calibrated for: ${it.reference.bounds.label} at ${distanceText(it.meters)} m" }
            ?: stableObservation?.let { "Ready to calibrate: ${it.bounds.label}" }
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
            Text("Calibrate distance")
        }
        if (calibration != null) {
            Button(onClick = { resetDistance() }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Clear calibration")
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
