package com.moeketsifaso.ntsamaise

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.ImageFormat
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.DataType
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal val BANKNOTE_CLASSES = listOf(
    "10 Loti",  // 0
    "10 Rand",  // 1
    "100 Loti", // 2
    "100 Rand", // 3
    "20 Loti",  // 4
    "20 Rand",  // 5
    "200 Loti", // 6
    "200 Rand", // 7
    "50 Loti",  // 8
    "50 Rand"   // 9
)

internal data class BanknoteDetection(
    val label: String,
    val confidence: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

internal data class CurrencyFeedback(
    val display: String,
    val speech: String,
    val detections: List<BanknoteDetection> = emptyList(),
    val previewBoxes: List<RectF> = emptyList()
)

internal object YoloPostProcessor {
    const val NUM_CLASSES = 10
    const val NUM_ANCHORS = 8400
    const val INPUT_SIZE = 640f

    fun calculateIoU(a: BanknoteDetection, b: BanknoteDetection): Float {
        val interLeft = max(a.left, b.left)
        val interTop = max(a.top, b.top)
        val interRight = min(a.right, b.right)
        val interBottom = min(a.bottom, b.bottom)

        val interArea = max(0f, interRight - interLeft) * max(0f, interBottom - interTop)
        if (interArea <= 0f) return 0f

        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)
        val unionArea = areaA + areaB - interArea
        return if (unionArea <= 0f) 0f else interArea / unionArea
    }

    fun nms(
        candidates: List<BanknoteDetection>,
        iouThreshold: Float = 0.45f
    ): List<BanknoteDetection> {
        val sorted = candidates.sortedByDescending { it.confidence }
        val selected = mutableListOf<BanknoteDetection>()

        for (candidate in sorted) {
            var shouldKeep = true
            for (kept in selected) {
                if (calculateIoU(candidate, kept) >= iouThreshold) {
                    shouldKeep = false
                    break
                }
            }
            if (shouldKeep) {
                selected.add(candidate)
            }
        }
        return selected
    }

    fun parseDetections(
        output: FloatArray,
        imageWidth: Int,
        imageHeight: Int,
        numAnchors: Int = NUM_ANCHORS,
        numClasses: Int = NUM_CLASSES,
        channelMajor: Boolean = true,
        inputWidth: Int = INPUT_SIZE.toInt(),
        inputHeight: Int = INPUT_SIZE.toInt(),
        confidenceThreshold: Float = 0.25f,
        iouThreshold: Float = 0.45f
    ): List<BanknoteDetection> {
        if (imageWidth <= 0 || imageHeight <= 0 || numAnchors <= 0 || numClasses != BANKNOTE_CLASSES.size || output.size != (4 + numClasses) * numAnchors) return emptyList()

        fun at(channel: Int, anchor: Int): Float = if (channelMajor) {
            output[channel * numAnchors + anchor]
        } else {
            output[anchor * (4 + numClasses) + channel]
        }

        var maxCoordinateMagnitude = 0f
        for (anchor in 0 until numAnchors) for (channel in 0..3) {
            val value = at(channel, anchor)
            if (value.isFinite()) maxCoordinateMagnitude = max(maxCoordinateMagnitude, kotlin.math.abs(value))
        }
        val coordinatesNormalized = maxCoordinateMagnitude <= 2f
        val coordinateScaleX = if (coordinatesNormalized) inputWidth.toFloat() else 1f
        val coordinateScaleY = if (coordinatesNormalized) inputHeight.toFloat() else 1f
        val xywh = maxCoordinateMagnitude <= 2f

        val scale = min(inputWidth / imageWidth.toFloat(), inputHeight / imageHeight.toFloat())
        val scaledW = imageWidth * scale
        val scaledH = imageHeight * scale
        val padX = (inputWidth - scaledW) / 2f
        val padY = (inputHeight - scaledH) / 2f

        val candidates = mutableListOf<BanknoteDetection>()

        for (anchor in 0 until numAnchors) {
            var bestClass = -1
            var maxScore = -1f

            for (c in 0 until NUM_CLASSES) {
                val score = at(4 + c, anchor)
                if (score > maxScore) {
                    maxScore = score
                    bestClass = c
                }
            }

            if (maxScore >= confidenceThreshold && bestClass in BANKNOTE_CLASSES.indices) {
                val a = at(0, anchor) * coordinateScaleX
                val b = at(1, anchor) * coordinateScaleY
                val c = at(2, anchor) * coordinateScaleX
                val d = at(3, anchor) * coordinateScaleY
                val cx = if (xywh) a else (a + c) / 2f
                val cy = if (xywh) b else (b + d) / 2f
                val w = if (xywh) c else c - a
                val h = if (xywh) d else d - b

                val x1 = (cx - w / 2f - padX) / scale
                val y1 = (cy - h / 2f - padY) / scale
                val x2 = (cx + w / 2f - padX) / scale
                val y2 = (cy + h / 2f - padY) / scale

                val left = (x1 / imageWidth).coerceIn(0f, 1f)
                val top = (y1 / imageHeight).coerceIn(0f, 1f)
                val right = (x2 / imageWidth).coerceIn(0f, 1f)
                val bottom = (y2 / imageHeight).coerceIn(0f, 1f)

                if (right > left && bottom > top) {
                    candidates.add(
                        BanknoteDetection(
                            label = BANKNOTE_CLASSES[bestClass],
                            confidence = maxScore,
                            left = left,
                            top = top,
                            right = right,
                            bottom = bottom
                        )
                    )
                }
            }
        }

        return nms(candidates, iouThreshold)
    }

    fun formatFeedback(detections: List<BanknoteDetection>): CurrencyFeedback {
        if (detections.isEmpty()) {
            return CurrencyFeedback(
                display = "No banknote recognized. Try another photo.",
                speech = "No banknote recognized",
                detections = emptyList()
            )
        }
        val primary = detections.first()
        val percent = (primary.confidence * 100).roundToInt()
        val display = if (detections.size == 1) {
            "${primary.label} detected ($percent%)"
        } else {
            "${primary.label} ($percent%) + ${detections.size - 1} more"
        }
        val speech = "${primary.label} detected"
        return CurrencyFeedback(display = display, speech = speech, detections = detections)
    }
}

/**
 * On-device YOLO11s currency detector using the bundled TensorFlow Lite export.
 */
internal class YoloCurrencyDetector(
    context: Context,
    modelAsset: String = "currency_yolo11s.tflite",
    private val confidenceThreshold: Float = 0.25f
) : CameraFrameProcessor<CurrencyFeedback> {
    private val interpreter = Interpreter(
        loadModelFile(context, modelAsset),
        Interpreter.Options().apply { setNumThreads(4) }
    )
    private val inputTensor = interpreter.getInputTensor(0)
    private val outputTensor = interpreter.getOutputTensor(0)
    private val inputShape = inputTensor.shape()
    private val outputShape = outputTensor.shape()
    private val inputIsNhwc = inputShape.size == 4 && inputShape[3] == 3
    private val inputHeight = if (inputIsNhwc) inputShape[1] else inputShape.getOrElse(2) { 0 }
    private val inputWidth = if (inputIsNhwc) inputShape[2] else inputShape.getOrElse(3) { 0 }
    private val outputChannels = outputShape.getOrNull(1)?.takeIf { it == 14 }
        ?: outputShape.getOrNull(2)?.takeIf { it == 14 } ?: 0
    private val outputAnchors = when {
        outputShape.size == 3 && outputShape[1] == outputChannels -> outputShape[2]
        outputShape.size == 3 && outputShape[2] == outputChannels -> outputShape[1]
        else -> 0
    }
    private val outputChannelMajor = outputShape.size == 3 && outputShape[1] == outputChannels
    private val outputArray = FloatArray(outputChannels * outputAnchors)
    private val outputBuffer = ByteBuffer.allocateDirect(outputTensor.numBytes()).order(ByteOrder.nativeOrder())
    private val inputBuffer = ByteBuffer.allocateDirect(inputTensor.numBytes()).order(ByteOrder.nativeOrder())
    private val inputValues = FloatArray(inputWidth * inputHeight * 3)
    private val pixelArrayForInput = IntArray(inputWidth * inputHeight)
    private var previewViewForFrame: androidx.camera.view.PreviewView? = null

    init {
        require(inputShape.size == 4 && inputShape[0] == 1 && inputWidth > 0 && inputHeight > 0 &&
            (inputIsNhwc || inputShape[1] == 3)) {
            "Unsupported currency model input shape: ${inputShape.contentToString()}"
        }
        require(inputTensor.dataType() == DataType.FLOAT32 || inputTensor.dataType() == DataType.UINT8 || inputTensor.dataType() == DataType.INT8) {
            "Unsupported currency model input type: ${inputTensor.dataType()}"
        }
        require(outputChannels == 14 && outputAnchors > 0 && outputShape[0] == 1) {
            "Unsupported currency model output shape: ${outputShape.contentToString()}"
        }
        require(outputTensor.dataType() == DataType.FLOAT32 || outputTensor.dataType() == DataType.UINT8 || outputTensor.dataType() == DataType.INT8) {
            "Unsupported currency model output type: ${outputTensor.dataType()}"
        }
    }

    override fun process(frame: ImageProxy): CurrencyFeedback {
        val bitmap = frame.toRgbaBitmap()
        var upright = bitmap
        try {
            if (frame.imageInfo.rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(frame.imageInfo.rotationDegrees.toFloat()) }
                upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }

            val scale = min(inputWidth.toFloat() / upright.width, inputHeight.toFloat() / upright.height)
            val scaledW = (upright.width * scale).roundToInt()
            val scaledH = (upright.height * scale).roundToInt()
            val padX = (inputWidth - scaledW) / 2f
            val padY = (inputHeight - scaledH) / 2f

            val letterbox = Bitmap.createBitmap(inputWidth, inputHeight, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(letterbox)
                canvas.drawColor(android.graphics.Color.rgb(114, 114, 114))
                val matrix = Matrix().apply {
                    postScale(scale, scale)
                    postTranslate(padX, padY)
                }
                canvas.drawBitmap(upright, matrix, Paint(Paint.FILTER_BITMAP_FLAG))

                letterbox.getPixels(pixelArrayForInput, 0, inputWidth, 0, 0, inputWidth, inputHeight)
                var valueIndex = 0
                if (inputIsNhwc) {
                    for (pixel in pixelArrayForInput) {
                        inputValues[valueIndex++] = ((pixel shr 16) and 0xFF) / 255f
                        inputValues[valueIndex++] = ((pixel shr 8) and 0xFF) / 255f
                        inputValues[valueIndex++] = (pixel and 0xFF) / 255f
                    }
                } else {
                    for (shift in intArrayOf(16, 8, 0)) for (pixel in pixelArrayForInput) {
                        inputValues[valueIndex++] = ((pixel shr shift) and 0xFF) / 255f
                    }
                }

                inputBuffer.clear()
                if (inputTensor.dataType() == DataType.FLOAT32) {
                    inputValues.forEach(inputBuffer::putFloat)
                } else {
                    val params = inputTensor.quantizationParams()
                    require(params.scale > 0f) { "Invalid quantized model input scale" }
                    for (value in inputValues) {
                        val quantized = (value / params.scale + params.zeroPoint).roundToInt()
                        val byteValue = if (inputTensor.dataType() == DataType.UINT8) quantized.coerceIn(0, 255) else quantized.coerceIn(-128, 127)
                        inputBuffer.put(byteValue.toByte())
                    }
                }
                inputBuffer.rewind()
                outputBuffer.clear()
                interpreter.run(inputBuffer, outputBuffer)
                outputBuffer.rewind()
                if (outputTensor.dataType() == DataType.FLOAT32) {
                    outputBuffer.asFloatBuffer().get(outputArray)
                } else {
                    val params = outputTensor.quantizationParams()
                    require(params.scale > 0f) { "Invalid quantized model output scale" }
                    for (i in outputArray.indices) {
                        val raw = outputBuffer.get().toInt()
                        val value = if (outputTensor.dataType() == DataType.UINT8) raw and 0xFF else raw
                        outputArray[i] = (value - params.zeroPoint) * params.scale
                    }
                }

                val detections = YoloPostProcessor.parseDetections(
                    output = outputArray,
                    imageWidth = upright.width,
                    imageHeight = upright.height,
                    numAnchors = outputAnchors,
                    numClasses = BANKNOTE_CLASSES.size,
                    channelMajor = outputChannelMajor,
                    inputWidth = inputWidth,
                    inputHeight = inputHeight,
                    confidenceThreshold = confidenceThreshold
                )

                return YoloPostProcessor.formatFeedback(detections)
            } finally {
                letterbox.recycle()
            }
        } finally {
            if (upright !== bitmap) upright.recycle()
            bitmap.recycle()
        }
    }

    override fun close() {
        interpreter.close()
    }

    override fun setPreviewView(view: androidx.camera.view.PreviewView) {
        previewViewForFrame = view
    }

    override fun processPreviewResult(
        result: CurrencyFeedback,
        frameTransform: androidx.camera.view.transform.OutputTransform?,
        previewView: androidx.camera.view.PreviewView
    ): CurrencyFeedback {
        val source = frameTransform ?: return result
        val target = previewView.outputTransform ?: return result
        val sourceToNormalized = android.graphics.Matrix().apply {
            source.matrix.invert(this)
        }
        val previewBoxes = result.detections.map { detection ->
            val rect = RectF(
                detection.left,
                detection.top,
                detection.right,
                detection.bottom
            )
            sourceToNormalized.mapRect(rect)
            androidx.camera.view.transform.CoordinateTransform(source, target).mapRect(rect)
            rect
        }
        return result.copy(previewBoxes = previewBoxes)
    }

    companion object {
        private fun loadModelFile(context: Context, modelAsset: String): MappedByteBuffer {
            val fileDescriptor = context.assets.openFd(modelAsset)
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                val channel = inputStream.channel
                return channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fileDescriptor.startOffset,
                    fileDescriptor.declaredLength
                )
            }
        }
    }
}

/** CameraX RGBA_8888 conversion honoring row and pixel stride. */
internal fun ImageProxy.toRgbaBitmap(): Bitmap {
    if (format == ImageFormat.YUV_420_888) return toYuvBitmap()
    if (planes.size >= 3) return toYuvBitmap()
    val plane = planes.firstOrNull() ?: error("Camera returned no image plane")
    val buffer = plane.buffer.duplicate()
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    require(pixelStride >= 4 && rowStride >= width * pixelStride) {
        "Unsupported camera image format=$format, planes=${planes.size}, rowStride=$rowStride, pixelStride=$pixelStride"
    }
    val baseOffset = buffer.position()
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val output = IntArray(width * height)
    for (y in 0 until height) {
        val row = y * rowStride
        for (x in 0 until width) {
            val offset = baseOffset + row + x * pixelStride
            require(offset + 3 < bytes.size) {
                "Camera RGBA plane is shorter than its declared row/pixel stride"
            }
            val r = bytes[offset].toInt() and 0xFF
            val g = bytes[offset + 1].toInt() and 0xFF
            val b = bytes[offset + 2].toInt() and 0xFF
            val a = bytes[offset + 3].toInt() and 0xFF
            output[y * width + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
}

/** Converts CameraX still-capture YUV_420_888 images to RGB. */
private fun ImageProxy.toYuvBitmap(): Bitmap {
    require(planes.size >= 3) { "Expected three YUV camera planes" }
    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]
    val yBuffer = yPlane.buffer.duplicate()
    val uBuffer = uPlane.buffer.duplicate()
    val vBuffer = vPlane.buffer.duplicate()
    val yBytes = ByteArray(yBuffer.remaining()).also(yBuffer::get)
    val uBytes = ByteArray(uBuffer.remaining()).also(uBuffer::get)
    val vBytes = ByteArray(vBuffer.remaining()).also(vBuffer::get)
    val pixels = IntArray(width * height)
    for (row in 0 until height) for (column in 0 until width) {
        val yIndex = row * yPlane.rowStride + column * yPlane.pixelStride
        val chromaRow = row / 2
        val chromaColumn = column / 2
        val uIndex = chromaRow * uPlane.rowStride + chromaColumn * uPlane.pixelStride
        val vIndex = chromaRow * vPlane.rowStride + chromaColumn * vPlane.pixelStride
        val y = ((yBytes[yIndex].toInt() and 0xFF) - 16).coerceAtLeast(0)
        val u = (uBytes[uIndex].toInt() and 0xFF) - 128
        val v = (vBytes[vIndex].toInt() and 0xFF) - 128
        val yy = y * 1192
        val red = (yy + 1634 * v).coerceIn(0, 262143)
        val green = (yy - 833 * v - 400 * u).coerceIn(0, 262143)
        val blue = (yy + 2066 * u).coerceIn(0, 262143)
        pixels[row * width + column] = (0xFF shl 24) or
            ((red shr 10) shl 16) or ((green shr 10) shl 8) or (blue shr 10)
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}
