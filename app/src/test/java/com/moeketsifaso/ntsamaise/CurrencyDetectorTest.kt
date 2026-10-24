package com.moeketsifaso.ntsamaise

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyDetectorTest {

    @Test
    fun banknoteClassesMatchTrainedYoloOrder() {
        assertEquals(10, BANKNOTE_CLASSES.size)
        assertEquals("10 Loti", BANKNOTE_CLASSES[0])
        assertEquals("10 Rand", BANKNOTE_CLASSES[1])
        assertEquals("100 Loti", BANKNOTE_CLASSES[2])
        assertEquals("100 Rand", BANKNOTE_CLASSES[3])
        assertEquals("20 Loti", BANKNOTE_CLASSES[4])
        assertEquals("20 Rand", BANKNOTE_CLASSES[5])
        assertEquals("200 Loti", BANKNOTE_CLASSES[6])
        assertEquals("200 Rand", BANKNOTE_CLASSES[7])
        assertEquals("50 Loti", BANKNOTE_CLASSES[8])
        assertEquals("50 Rand", BANKNOTE_CLASSES[9])
    }

    @Test
    fun calculateIoUHandlesIdenticalAndDisjointBoxes() {
        val boxA = BanknoteDetection("10 Loti", 0.9f, 0.1f, 0.1f, 0.5f, 0.5f)
        val boxIdentical = BanknoteDetection("10 Loti", 0.8f, 0.1f, 0.1f, 0.5f, 0.5f)
        val boxDisjoint = BanknoteDetection("50 Loti", 0.7f, 0.6f, 0.6f, 0.9f, 0.9f)

        assertEquals(1.0f, YoloPostProcessor.calculateIoU(boxA, boxIdentical), 0.001f)
        assertEquals(0.0f, YoloPostProcessor.calculateIoU(boxA, boxDisjoint), 0.001f)
    }

    @Test
    fun calculateIoUHandlesPartialOverlap() {
        val boxA = BanknoteDetection("10 Loti", 0.9f, 0.0f, 0.0f, 0.5f, 0.5f) // area = 0.25
        val boxB = BanknoteDetection("10 Loti", 0.8f, 0.25f, 0.0f, 0.75f, 0.5f) // area = 0.25, inter = 0.25 * 0.5 = 0.125
        // union = 0.25 + 0.25 - 0.125 = 0.375. IoU = 0.125 / 0.375 = 1/3
        assertEquals(1f / 3f, YoloPostProcessor.calculateIoU(boxA, boxB), 0.001f)
    }

    @Test
    fun nmsSuppressesRedundantOverlappingBoxes() {
        val highConf = BanknoteDetection("10 Loti", 0.92f, 0.1f, 0.1f, 0.6f, 0.6f)
        val redundant = BanknoteDetection("10 Loti", 0.75f, 0.12f, 0.11f, 0.59f, 0.61f)
        val separate = BanknoteDetection("100 Rand", 0.85f, 0.7f, 0.7f, 0.95f, 0.95f)

        val filtered = YoloPostProcessor.nms(listOf(redundant, highConf, separate), iouThreshold = 0.45f)
        assertEquals(2, filtered.size)
        assertEquals(highConf, filtered[0])
        assertEquals(separate, filtered[1])
    }

    @Test
    fun parseDetectionsDecodesAnchorsAndUnLetterboxes() {
        val output = FloatArray(14 * 8400)
        val anchorIdx = 42

        // Letterbox setup: 640x480 image into 640x640 requires scale = 1.0, padX = 0, padY = 80
        val imageW = 640
        val imageH = 480
        val cx = 320f
        val cy = 240f + 80f // 320 in 640x640 space -> 240 in unpadded 480 image space
        val w = 200f
        val h = 100f

        output[0 * 8400 + anchorIdx] = cx
        output[1 * 8400 + anchorIdx] = cy
        output[2 * 8400 + anchorIdx] = w
        output[3 * 8400 + anchorIdx] = h
        // Class 4 is "20 Loti"
        output[(4 + 4) * 8400 + anchorIdx] = 0.88f

        val detections = YoloPostProcessor.parseDetections(
            output = output,
            imageWidth = imageW,
            imageHeight = imageH,
            confidenceThreshold = 0.50f
        )

        assertEquals(1, detections.size)
        val det = detections[0]
        assertEquals("20 Loti", det.label)
        assertEquals(0.88f, det.confidence, 0.001f)
        // x1 = 320 - 100 = 220 -> left = 220 / 640
        assertEquals(220f / 640f, det.left, 0.01f)
        // y1 = 320 - 50 - 80 = 190 -> top = 190 / 480
        assertEquals(190f / 480f, det.top, 0.01f)
        // x2 = 320 + 100 = 420 -> right = 420 / 640
        assertEquals(420f / 640f, det.right, 0.01f)
        // y2 = 320 + 50 - 80 = 290 -> bottom = 290 / 480
        assertEquals(290f / 480f, det.bottom, 0.01f)
    }

    @Test
    fun parseDetectionsScalesNormalizedModelCoordinates() {
        val anchors = 2
        val output = FloatArray(14 * anchors)
        val anchor = 0
        output[0 * anchors + anchor] = 0.5f
        output[1 * anchors + anchor] = 0.5f
        output[2 * anchors + anchor] = 0.25f
        output[3 * anchors + anchor] = 0.2f
        output[(4 + 6) * anchors + anchor] = 0.40f

        val detections = YoloPostProcessor.parseDetections(
            output = output,
            imageWidth = 640,
            imageHeight = 640,
            numAnchors = anchors,
            confidenceThreshold = 0.25f
        )

        assertEquals(1, detections.size)
        assertEquals("200 Loti", detections.single().label)
        assertEquals(0.375f, detections.single().left, 0.001f)
        assertEquals(0.625f, detections.single().right, 0.001f)
    }

    @Test
    fun parseDetectionsSupportsAnchorMajorOutput() {
        val anchors = 3
        val output = FloatArray(14 * anchors)
        val anchor = 1
        output[anchor * 14] = 320f
        output[anchor * 14 + 1] = 320f
        output[anchor * 14 + 2] = 160f
        output[anchor * 14 + 3] = 80f
        output[anchor * 14 + 4 + 7] = 0.91f

        val detections = YoloPostProcessor.parseDetections(
            output, 640, 640, numAnchors = anchors, channelMajor = false, confidenceThreshold = 0.5f
        )

        assertEquals(1, detections.size)
        assertEquals("200 Rand", detections.single().label)
        assertEquals(0.91f, detections.single().confidence, 0.001f)
    }

    @Test
    fun parseDetectionsIgnoresBelowThreshold() {
        val output = FloatArray(14 * 8400)
        output[(4 + 2) * 8400 + 10] = 0.30f // Below default 0.45 threshold

        val detections = YoloPostProcessor.parseDetections(output, 640, 480, confidenceThreshold = 0.45f)
        assertTrue(detections.isEmpty())
    }

    @Test
    fun formatFeedbackHandlesEmptyAndSingleDetections() {
        val emptyFeedback = YoloPostProcessor.formatFeedback(emptyList())
        assertEquals("Looking for banknotes...", emptyFeedback.display)
        assertEquals("No banknote recognized", emptyFeedback.speech)

        val singleDetection = BanknoteDetection("50 Loti", 0.912f, 0.1f, 0.1f, 0.8f, 0.8f)
        val singleFeedback = YoloPostProcessor.formatFeedback(listOf(singleDetection))
        assertEquals("50 Loti detected (91%)", singleFeedback.display)
        assertEquals("50 Loti detected", singleFeedback.speech)
    }

    @Test
    fun allBanknoteClassesHaveOfflineSesothoTranslations() = runBlocking {
        for (label in BANKNOTE_CLASSES) {
            val speechText = "$label detected"
            val result = GoogleCloudTranslator.translate(speechText)
            assertEquals(TranslationSource.LOCAL_DICTIONARY, result.source)
            assertTrue("Expected Sesotho translation for $speechText", result.translatedText.isNotBlank())
            assertTrue("Translation should differ from English original", result.translatedText != speechText)
        }
    }

    @Test
    fun banknoteDetectionWithPercentageTranslatesOffline() = runBlocking {
        val text = "10 Loti detected (85%)"
        val result = GoogleCloudTranslator.translate(text)
        assertEquals(TranslationSource.LOCAL_DICTIONARY, result.source)
        assertEquals("Maloti a 10 a bonoe (85%)", result.translatedText)
    }

    @Test
    fun uiTranslationsForBanknoteFlowArePresent() {
        val uiEn = "Point camera at a Lesotho or Rand banknote."
        val uiSt = AppLanguage.SESOTHO.ui(uiEn)
        assertEquals("Lebisa khamera cheleteng ea pampiri ea Lesotho kapa ea Ranta.", uiSt)

        val noteSt = AppLanguage.SESOTHO.ui("Looking for banknotes...")
        assertEquals("E batla chelete...", noteSt)
    }
}
