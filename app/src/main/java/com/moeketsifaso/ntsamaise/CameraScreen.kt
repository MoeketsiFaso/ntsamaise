package com.moeketsifaso.ntsamaise

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var detectedObject by remember { mutableStateOf("Looking for objects...") }
    var spokenResult by remember { mutableStateOf<String?>(null) }
    val speechError = rememberSpokenDetection(spokenResult)
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val previewView = remember(context) { PreviewView(context) }
    DisposableEffect(hasCameraPermission, lifecycleOwner, previewView) {
        val disposed = AtomicBoolean(false)
        val executor = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var detector: SurroundingsDetector? = null // Accessed only on executor.
        var provider: ProcessCameraProvider? = null
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val analysis = ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        fun reportError(message: String, error: Exception) {
            Log.e("SurroundingsDetector", message, error)
            mainExecutor.execute {
                if (!disposed.get()) {
                    detectedObject = message
                    spokenResult = null
                }
            }
        }

        if (hasCameraPermission) {
            executor.execute {
                if (!disposed.get()) {
                    try {
                        detector = SurroundingsDetector(context.applicationContext)
                    } catch (error: Exception) {
                        reportError("Object detection unavailable. Reopen the scanner to retry.", error)
                    }
                }
            }
            analysis.setAnalyzer(executor) { frame ->
                try {
                    if (!disposed.get()) {
                        detector?.detect(frame)?.let { feedback ->
                            mainExecutor.execute {
                                if (!disposed.get()) {
                                    detectedObject = feedback.display
                                    spokenResult = feedback.speech
                                }
                            }
                        }
                    }
                } catch (error: Exception) {
                    reportError("Detection error. Try pointing the camera again.", error)
                } finally {
                    frame.close()
                }
            }
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                if (!disposed.get()) {
                    try {
                        provider = future.get()
                        provider?.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                        )
                    } catch (error: Exception) {
                        analysis.clearAnalyzer()
                        reportError("Camera unavailable. Reopen the scanner to retry.", error)
                    }
                }
            }, mainExecutor)
        }

        onDispose {
            disposed.set(true)
            analysis.clearAnalyzer()
            provider?.unbind(preview, analysis)
            // Queue cleanup after any frame already being processed.
            executor.execute { detector?.close() }
            executor.shutdown()
        }
    }

    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            Text(
                text = detectedObject,
                fontSize = 22.sp,
                color = Color.White,
                modifier = Modifier.padding(20.dp)
                    .background(Color.Black.copy(alpha = 0.65f)).padding(12.dp)
            )
            speechError?.let { message ->
                Text(
                    text = message,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
                        .background(Color.Black.copy(alpha = 0.85f)).padding(12.dp)
                )
            }
        }
    } else {
        Text(text = "Camera permission is required.")
    }
}
