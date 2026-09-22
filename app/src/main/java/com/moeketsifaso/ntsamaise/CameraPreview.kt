package com.moeketsifaso.ntsamaise

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** All processor calls are serialized on a worker; null means this frame was skipped. */
internal interface CameraFrameProcessor<T> : AutoCloseable {
    fun process(frame: ImageProxy): T?
}

/** Camera/permission ownership shared by object scanning and text reading. */
@Composable
internal fun <T> CameraPreview(
    createProcessor: () -> CameraFrameProcessor<T>,
    onResult: (T) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    onReady: (Boolean) -> Unit = {},
    imageFormat: Int = ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888,
    resolution: Size? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentFactory by rememberUpdatedState(createProcessor)
    val currentResult by rememberUpdatedState(onResult)
    val currentError by rememberUpdatedState(onError)
    val currentReady by rememberUpdatedState(onReady)
    fun permissionGranted() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    var hasPermission by remember { mutableStateOf(permissionGranted()) }
    var resumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }
    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (resumed) hasPermission = permissionGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val previewView = remember(context) {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FIT_CENTER }
    }
    DisposableEffect(hasPermission, resumed, lifecycleOwner, previewView, imageFormat, resolution) {
        currentReady(false)
        if (!hasPermission || !resumed) {
            onDispose { }
        } else {
            val disposed = AtomicBoolean(false)
            val executor = Executors.newSingleThreadExecutor()
            val mainExecutor = ContextCompat.getMainExecutor(context)
            var processor: CameraFrameProcessor<T>? = null
            var provider: ProcessCameraProvider? = null
            var processorReady = false // The readiness flags are main-thread only.
            var cameraBound = false
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setOutputImageFormat(imageFormat)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .apply {
                    if (resolution != null) {
                        setResolutionSelector(ResolutionSelector.Builder()
                            .setResolutionStrategy(ResolutionStrategy(
                                resolution, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )).build())
                    }
                }.build()

            fun reportError(message: String, error: Exception) {
                Log.e("CameraPreview", message, error)
                mainExecutor.execute { if (!disposed.get()) currentError(message) }
            }

            executor.execute {
                if (!disposed.get()) {
                    try {
                        processor = currentFactory()
                        mainExecutor.execute {
                            if (!disposed.get()) {
                                processorReady = true
                                currentReady(cameraBound)
                            }
                        }
                    } catch (error: Exception) {
                        reportError("Recognition unavailable. Reopen this screen to retry.", error)
                    }
                }
            }
            analysis.setAnalyzer(executor) { frame ->
                try {
                    if (!disposed.get()) {
                        processor?.process(frame)?.let { result ->
                            mainExecutor.execute {
                                if (!disposed.get()) currentResult(result)
                            }
                        }
                    }
                } catch (error: Exception) {
                    reportError("Could not recognize this image. Try again with the camera steady.", error)
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
                        cameraBound = true
                        currentReady(processorReady)
                    } catch (error: Exception) {
                        analysis.clearAnalyzer()
                        reportError("Camera unavailable. Reopen this screen to retry.", error)
                    }
                }
            }, mainExecutor)

            onDispose {
                disposed.set(true)
                currentReady(false)
                analysis.clearAnalyzer()
                provider?.unbind(preview, analysis)
                // OCR may still be using a frame. Finish it before closing its recognizer.
                executor.execute { processor?.close() }
                executor.shutdown()
            }
        }
    }

    if (hasPermission) {
        AndroidView(factory = { previewView }, modifier = modifier)
    } else {
        Column(modifier) {
            Text("Camera permission is required. If denied permanently, enable it in the app's settings.")
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text("Allow camera")
            }
        }
    }
}
