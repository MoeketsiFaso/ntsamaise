package com.moeketsifaso.ntsamaise

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ObstacleDetectionTest {
    private fun box(left: Float, right: Float, top: Float = 0.2f, bottom: Float = 0.9f,
                    score: Float = 0.8f, label: String = "chair") =
        DetectedObject(label, score, left, top, right, bottom)

    @Test fun directionsUseUprightImageBounds() {
        assertEquals(ObstacleDirection.LEFT, selectObstacle(listOf(box(0f, 0.3f)))?.direction)
        assertEquals(ObstacleDirection.AHEAD, selectObstacle(listOf(box(0.4f, 0.6f)))?.direction)
        assertEquals(ObstacleDirection.RIGHT, selectObstacle(listOf(box(0.7f, 1f)))?.direction)
    }

    @Test fun wideBoxIntersectingCenterIsAheadEvenWhenItsCenterIsToTheSide() {
        assertEquals(ObstacleDirection.AHEAD, selectObstacle(listOf(box(0f, 0.5f)))?.direction)
    }

    @Test fun centralCandidateTakesPriorityOverLargerPeripheralObject() {
        val result = selectObstacle(listOf(box(0f, 0.3f, label = "car"), box(0.4f, 0.6f)))
        assertEquals("chair", result?.label)
    }

    @Test fun largestCentralCandidateWinsRegardlessOfInputOrder() {
        val objects = listOf(box(0.4f, 0.6f), box(0.2f, 0.8f, label = "person"))
        assertEquals("person", selectObstacle(objects)?.label)
        assertEquals(selectObstacle(objects), selectObstacle(objects.reversed()))
    }

    @Test fun rejectsTinyLowConfidenceMalformedAndNonFiniteBoxes() {
        for (candidate in listOf(box(0.4f, 0.41f), box(0f, 1f, score = 0.49f),
            box(0.8f, 0.2f), box(0f, 1f, top = 0.9f, bottom = 0.2f),
            box(Float.NaN, 1f), box(0f, 1f, score = Float.NaN), box(0f, 1f, label = ""),
            box(2f, 3f))) {
            assertNull(selectObstacle(listOf(candidate)))
        }
    }

    @Test fun clipsPartiallyVisibleBoxesToImage() {
        val result = selectObstacle(listOf(box(-1f, 0.2f, top = -1f, bottom = 0.5f)))
        assertEquals(ObstacleDirection.LEFT, result?.direction)
        assertEquals(0.1f, result!!.area, 0.001f)
    }

    @Test fun missingDetectionsNeverAnnounceThatPathIsClear() {
        val result = obstacleFeedback(emptyList())
        assertEquals("", result.speech)
        assertTrue(result.display.contains("does not mean the path is clear"))
    }

    @Test fun changingObjectLabelsInSameDirectionDoesNotChangeSpokenAlert() {
        assertEquals(obstacleFeedback(listOf(box(0f, 1f))).speech,
            obstacleFeedback(listOf(box(0f, 1f, label = "person"))).speech)
    }

    @Test fun everyDirectionalAlertHasOfflineSesothoTranslation() = runBlocking {
        GoogleCloudTranslator.clearCache()
        for (direction in ObstacleDirection.entries) {
            val result = GoogleCloudTranslator.translate(direction.prompt)
            assertEquals(TranslationSource.LOCAL_DICTIONARY, result.source)
            assertNotEquals(direction.prompt, result.translatedText)
        }
        GoogleCloudTranslator.clearCache()
    }

    @Test fun obstacleAnnouncementIntervalsLimitChangesAndRepeats() {
        val gate = AnnouncementGate(1_500, 5_000)
        gate.record("ahead", 100)
        assertFalse(gate.canAnnounce("left", 1_599))
        assertTrue(gate.canAnnounce("left", 1_600))
        assertFalse(gate.canAnnounce("ahead", 5_099))
        assertTrue(gate.canAnnounce("ahead", 5_100))
    }
}
