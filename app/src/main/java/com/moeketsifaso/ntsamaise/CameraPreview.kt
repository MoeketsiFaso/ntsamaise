package com.moeketsifaso.ntsamaise

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.util.Log
import android.util.Size
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.transform.CoordinateTransform
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
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
    fun setPreviewView(view: PreviewView) = Unit
    fun processPreviewResult(
        result: T,
        frameTransform: androidx.camera.view.transform.OutputTransform?,
        previewView: PreviewView
    ): T? = result
}

/** Camera/permission ownership shared by object scanning and text reading. */
@Composable
internal fun <T> CameraPreview(
    createProcessor: () -> CameraFrameProcessor<T>,
    onResult: (T) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    onReady: (Boolean) -> Unit = {},
    captureMode: Boolean = false,
    captureEnabled: Boolean = true,
    onCaptureState: (Boolean) -> Unit = {},
    onCaptureAction: ((() -> Unit)?) -> Unit = {},
    imageFormat: Int = ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888,
    resolution: Size? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentFactory by rememberUpdatedState(createProcessor)
    val currentResult by rememberUpdatedState(onResult)
    val currentError by rememberUpdatedState(onError)
    val currentReady by rememberUpdatedState(onReady)
    val currentCaptureState by rememberUpdatedState(onCaptureState)
    val currentCaptureAction by rememberUpdatedState(onCaptureAction)
    val currentCaptureMode by rememberUpdatedState(captureMode)
    val currentCaptureEnabled by rememberUpdatedState(captureEnabled)
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
    DisposableEffect(hasPermission, resumed, lifecycleOwner, previewView, imageFormat, resolution, captureMode) {
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
            val captureInProgress = AtomicBoolean(false)
            var imageCapture: ImageCapture? = null
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

            fun reportError(message: String, error: Throwable) {
                Log.e("CameraPreview", message, error)
                mainExecutor.execute { if (!disposed.get()) currentError(message) }
            }

            fun captureStill() {
                if (!currentCaptureEnabled || disposed.get() || !captureInProgress.compareAndSet(false, true)) return
                val capture = imageCapture
                if (capture == null) {
                    captureInProgress.set(false)
                    return
                }
                currentCaptureState(true)
                try {
                    capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        try {
                            val result = processor?.process(image)
                            val resultTransform = if (result != null) runCatching {
                                ImageProxyTransformFactory().apply {
                                    setUsingCropRect(true)
                                    setUsingRotationDegrees(true)
                                }.getOutputTransform(image)
                            }.getOrNull() else null
                            if (result != null) mainExecutor.execute {
                                if (!disposed.get()) {
                                    try {
                                        val displayResult = processor?.processPreviewResult(
                                            result, resultTransform, previewView
                                        )
                                        currentResult(displayResult ?: result)
                                    } catch (error: Throwable) {
                                        reportError("Could not display recognition results.", error)
                                    }
                                }
                            }
                        } catch (error: Throwable) {
                            reportError("Could not recognize this image. Try again with the camera steady.", error)
                        } finally {
                            image.close()
                            mainExecutor.execute {
                                captureInProgress.set(false)
                                if (!disposed.get()) currentCaptureState(false)
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        reportError("Could not capture a photo. Try again.", exception)
                        mainExecutor.execute {
                            captureInProgress.set(false)
                            if (!disposed.get()) currentCaptureState(false)
                        }
                    }
                    })
                } catch (error: Throwable) {
                    captureInProgress.set(false)
                    currentCaptureState(false)
                    reportError("Could not capture a photo. Try again.", error)
                }
            }

            fun installAnalyzer() {
                analysis.setAnalyzer(executor) { frame ->
                    try {
                        if (!disposed.get() && processorReady) {
                            val result = processor?.process(frame)
                            result?.let {
                                mainExecutor.execute {
                                    if (!disposed.get()) currentResult(it)
                                }
                            }
                        }
                    } catch (error: Throwable) {
                        reportError("Could not recognize this image. Try again with the camera steady.", error)
                    } finally {
                        frame.close()
                    }
                }
            }
            executor.execute {
                if (!disposed.get()) {
                    try {
                        processor = currentFactory()
                        processor?.setPreviewView(previewView)
                        mainExecutor.execute {
                            if (!disposed.get()) {
                                processorReady = true
                                if (cameraBound && !currentCaptureMode) installAnalyzer()
                                if (cameraBound && currentCaptureMode) currentCaptureAction(::captureStill)
                                currentReady(cameraBound)
                            }
                        }
                    } catch (error: Throwable) {
                        reportError("Recognition unavailable. Reopen this screen to retry.", error)
                    }
                }
            }
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                if (!disposed.get()) {
                    try {
                        provider = future.get()
                        if (currentCaptureMode) {
                            val capture = ImageCapture.Builder()
                                .setBufferFormat(ImageFormat.YUV_420_888)
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture
                            provider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                        } else {
                            provider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                        }
                        cameraBound = true
                        if (processorReady && currentCaptureMode) currentCaptureAction(::captureStill)
                        if (processorReady && !currentCaptureMode) installAnalyzer()
                        currentReady(processorReady)
                    } catch (error: Throwable) {
                        analysis.clearAnalyzer()
                        reportError("Camera unavailable. Reopen this screen to retry.", error)
                    }
                }
            }, mainExecutor)

            onDispose {
                disposed.set(true)
                currentReady(false)
                currentCaptureAction(null)
                analysis.clearAnalyzer()
                if (currentCaptureMode) imageCapture?.let { provider?.unbind(preview, it) }
                else provider?.unbind(preview, analysis)
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
            UiText("Camera permission is required. If denied permanently, enable it in the app's settings.")
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                UiText("Allow camera")
            }
        }
    }
}

internal fun mapFrameRectToPreview(rect: RectF, frame: ImageProxy, previewView: PreviewView): RectF {
    val source = ImageProxyTransformFactory().apply {
        setUsingCropRect(true)
        setUsingRotationDegrees(true)
    }.getOutputTransform(frame)
    val target = previewView.outputTransform ?: return RectF(rect)
    return RectF(rect).also { CoordinateTransform(source, target).mapRect(it) }
}
