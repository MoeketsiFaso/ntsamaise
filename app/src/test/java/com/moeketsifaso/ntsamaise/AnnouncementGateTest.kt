package com.moeketsifaso.ntsamaise

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnouncementGateTest {
    @Test
    fun unchangedSceneRepeatsOnlyAfterEightSeconds() {
        val gate = AnnouncementGate()
        assertTrue(gate.canAnnounce("Food detected", 0))
        gate.record("Food detected", 0)
        assertFalse(gate.canAnnounce("Food detected", 7_999))
        assertTrue(gate.canAnnounce("Food detected", 8_000))
    }

    @Test
    fun changingLabelsCannotFloodSpeech() {
        val gate = AnnouncementGate()
        gate.record("Food detected", 100)
        assertFalse(gate.canAnnounce("Plant detected", 3_099))
        assertTrue(gate.canAnnounce("Plant detected", 3_100))
        gate.record("Plant detected", 3_100)
        assertFalse(gate.canAnnounce("Food detected", 3_200))
    }

    @Test
    fun returningToScannerAllowsImmediateAnnouncement() {
        val gate = AnnouncementGate()
        gate.record("Food detected", 100)
        gate.reset()
        assertTrue(gate.canAnnounce("Food detected", 200))
    }
}
