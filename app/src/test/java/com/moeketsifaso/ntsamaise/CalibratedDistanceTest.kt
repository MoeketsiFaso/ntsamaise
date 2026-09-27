package com.moeketsifaso.ntsamaise

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CalibratedDistanceTest {
    private fun observation(height: Float = 0.6f, label: String = "chair") = ObstacleObservation(
        DetectedObject(label, 0.9f, 0.3f, 0.1f, 0.7f, 0.1f + height),
        FrameGeometry(480, 640, 90))

    @Test fun halvingImageHeightDoublesCalibratedDistance() {
        assertEquals(4f, estimateDistance(DistanceCalibration(observation(), 2f), observation(0.3f))!!, 0.001f)
    }

    @Test fun doublingImageHeightHalvesCalibratedDistance() {
        assertEquals(1f, estimateDistance(DistanceCalibration(observation(0.3f), 2f), observation())!!, 0.001f)
    }

    @Test fun rejectsDifferentClassOrCameraGeometry() {
        val calibration = DistanceCalibration(observation(), 2f)
        assertNull(estimateDistance(calibration, observation(label = "person")))
        assertNull(estimateDistance(calibration, observation().copy(geometry = FrameGeometry(640, 480, 0))))
    }

    @Test fun rejectsClippedBoxesLowConfidenceAndInvalidValues() {
        val original = observation()
        for (bounds in listOf(original.bounds.copy(top = 0f), original.bounds.copy(left = 0.99f),
            original.bounds.copy(confidence = 0.6f), original.bounds.copy(bottom = Float.NaN),
            original.bounds.copy(bottom = 0.05f), original.bounds.copy(confidence = Float.POSITIVE_INFINITY))) {
            assertNull(estimateDistance(DistanceCalibration(original, 2f), original.copy(bounds = bounds)))
        }
    }

    @Test fun rejectsInvalidReferenceAndOutOfRangeEstimatesWithoutClamping() {
        for (distance in listOf(0f, -1f, 11f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertNull(estimateDistance(DistanceCalibration(observation(), distance), observation()))
        }
        assertNull(estimateDistance(DistanceCalibration(observation(), 10f), observation(0.2f)))
        assertNull(estimateDistance(DistanceCalibration(observation(0.2f), 0.3f), observation()))
        assertNull(estimateDistance(DistanceCalibration(observation().copy(
            bounds = observation().bounds.copy(top = 0f)), 2f), observation()))
    }

    @Test fun requiresMultipleSamplesAndSixHundredMilliseconds() {
        val window = DistanceObservationWindow()
        assertNull(window.add(observation(), 0))
        assertNull(window.add(observation(), 300))
        assertNotNull(window.add(observation(), 600))
    }

    @Test fun unstableOrMissingDetectionsDoNotProduceEstimates() {
        val window = DistanceObservationWindow()
        window.add(observation(), 0)
        window.add(observation(0.3f), 300)
        assertNull(window.add(observation(), 600))
        assertNull(window.add(null, 700))
        assertNull(window.add(observation(), 800))
    }

    @Test fun gapsObjectChangesAndExplicitClearsResetTheWindow() {
        for (change in listOf("gap", "class", "clear")) {
            val window = DistanceObservationWindow()
            window.add(observation(), 0)
            window.add(observation(), 300)
            assertNotNull(window.add(observation(), 600))
            if (change == "clear") window.clear()
            assertNull(window.add(observation(label = if (change == "class") "person" else "chair"),
                if (change == "gap") 2_000 else 700))
        }
    }

    @Test fun smallJitterKeepsSameSpeechDistanceAndPromptsTranslateOffline() = runBlocking {
        GoogleCloudTranslator.clearCache()
        for (direction in ObstacleDirection.entries) {
            val prompt = distanceSpeech(direction.prompt, 2.01f)
            assertEquals(prompt, distanceSpeech(direction.prompt, 1.99f))
            val translated = GoogleCloudTranslator.translate(prompt)
            assertEquals(TranslationSource.LOCAL_DICTIONARY, translated.source)
            assertTrue(translated.translatedText.contains("2.0"))
            assertFalse(translated.translatedText.contains("Estimated distance"))
        }
        GoogleCloudTranslator.clearCache()
    }
}
