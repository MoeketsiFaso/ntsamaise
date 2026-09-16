package com.moeketsifaso.ntsamaise

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScreen() {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var detectedObject by remember {
        mutableStateOf("Looking for objects...")
    }

    var spokenResult by remember { mutableStateOf<String?>(null) }
    val speechError = rememberSpokenDetection(spokenResult)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val cameraExecutor = remember {
        Executors.newSingleThreadExecutor()
    }

    val detectorOptions = remember {
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    }

    val objectDetector = remember {
        ObjectDetection.getClient(detectorOptions)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            objectDetector.close()
        }
    }

    if (hasCameraPermission) {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            AndroidView(
                modifier = Modifier.fillMaxSize(),

                factory = { androidContext ->

                    val previewView = PreviewView(androidContext)

                    val cameraProviderFuture =
                        ProcessCameraProvider.getInstance(androidContext)

                    cameraProviderFuture.addListener(
                        {

                            val cameraProvider =
                                cameraProviderFuture.get()

                            val preview = Preview.Builder()
                                .build()
                                .also {
                                    it.surfaceProvider =
                                        previewView.surfaceProvider
                                }

                            val imageAnalysis =
                                ImageAnalysis.Builder()
                                    .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                    )
                                    .build()

                            imageAnalysis.setAnalyzer(
                                cameraExecutor
                            ) { imageProxy ->

                                val mediaImage =
                                    imageProxy.image

                                if (mediaImage != null) {

                                    val inputImage =
                                        InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )

                                    objectDetector.process(inputImage)
                                        .addOnSuccessListener { objects ->

                                            if (objects.isEmpty()) {

                                                detectedObject =
                                                    "No object detected"
                                                spokenResult = "No object detected"

                                            } else {

                                                val firstObject =
                                                    objects.first()

                                                if (
                                                    firstObject.labels.isNotEmpty()
                                                ) {

                                                    val label =
                                                        firstObject.labels.first()

                                                    detectedObject =
                                                        "${label.text} ${
                                                            (label.confidence * 100).toInt()
                                                        }%"
                                                    spokenResult = "${label.text} detected"

                                                } else {

                                                    detectedObject =
                                                        "Object detected"
                                                    spokenResult = "Object detected"
                                                }
                                            }
                                        }
                                        .addOnFailureListener {

                                            detectedObject =
                                                "Detection error"
                                            spokenResult = null

                                        }
                                        .addOnCompleteListener {

                                            imageProxy.close()
                                        }

                                } else {

                                    imageProxy.close()
                                }
                            }

                            val cameraSelector =
                                CameraSelector.DEFAULT_BACK_CAMERA

                            try {

                                cameraProvider.unbindAll()

                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )

                            } catch (exception: Exception) {

                                exception.printStackTrace()
                            }

                        },

                        ContextCompat.getMainExecutor(
                            androidContext
                        )
                    )

                    previewView
                }
            )

            Text(
                text = detectedObject,
                fontSize = 22.sp,
                color = Color.White,
                modifier = Modifier
                    .padding(20.dp)
                    .background(
                        Color.Black.copy(alpha = 0.65f)
                    )
                    .padding(12.dp)
            )

            speechError?.let { message ->
                Text(
                    text = message,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp)
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(12.dp)
                )
            }
        }

    } else {

        Text(
            text = "Camera permission is required."
        )
    }
}
