package com.moeketsifaso.ntsamaise

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ObjectColourTest {
    private val box = DetectedObject("chair", 0.9f, 0.2f, 0.2f, 0.8f, 0.8f)
    private val geometry = FrameGeometry(100, 100, 0)
    private fun rgb(value: Int) = value or (0xff shl 24)
    private val red = ColourEstimate(ObjectColour.RED, false)
    private val blue = ColourEstimate(ObjectColour.BLUE, false)

    @Test fun recognisesRepresentativeColoursIncludingNeutralAndWarmTones() {
        val examples = mapOf(
            0x080808 to ObjectColour.BLACK, 0xf5f5f5 to ObjectColour.WHITE,
            0x808080 to ObjectColour.GREY, 0xe02020 to ObjectColour.RED,
            0xff8800 to ObjectColour.ORANGE, 0xeeee10 to ObjectColour.YELLOW,
            0x20a040 to ObjectColour.GREEN, 0x2040cc to ObjectColour.BLUE,
            0x8020c0 to ObjectColour.PURPLE, 0xffa0c0 to ObjectColour.PINK,
            0x805020 to ObjectColour.BROWN
        )
        for ((pixel, expected) in examples) assertEquals(expected, classifyColour(rgb(pixel)))
    }

    @Test fun samplesSelectedBoxCentreInsteadOfFrameOrBoxBorder() {
        var samples = 0
        val result = estimateObjectColour(100, 100, box) { x, y ->
            samples++
            if (x in 32..67 && y in 32..67) rgb(0xee2020) else rgb(0x0020ee)
        }
        assertEquals(red, result)
        assertEquals(576, samples)
    }

    @Test fun dominantColourSurvivesSmallHighlightAndMixturesReturnLowConfidenceGuess() {
        assertEquals(ColourEstimate(ObjectColour.GREEN, false), estimateObjectColour(100, 100, box) { x, _ ->
            if (x < 60) rgb(0x20a040) else rgb(0xffffff)
        })
        val mixed = estimateObjectColour(100, 100, box) { x, _ ->
            if (x < 50) rgb(0xff0000) else rgb(0x0000ff)
        }!!
        assertTrue(mixed.colour in listOf(ObjectColour.RED, ObjectColour.BLUE))
        assertTrue(mixed.lowConfidence)
    }

    @Test fun rejectsOffImageTinyInvertedAndNonFiniteRegionsWithoutReadingPixels() {
        for (invalid in listOf(box.copy(left = -1f, right = -0.1f), box.copy(left = 1.1f, right = 1.5f),
            box.copy(top = Float.NaN), box.copy(bottom = Float.POSITIVE_INFINITY),
            box.copy(left = 0.9f), box.copy(right = 0.21f), box.copy(bottom = 0.21f))) {
            assertNull(estimateObjectColour(100, 100, invalid) { _, _ -> error("Invalid region sampled") })
        }
        assertNull(estimateObjectColour(0, 100, box) { _, _ -> error("Empty image sampled") })
    }

    @Test fun fullImageBoundsSampleSafelyAndTransparentPixelsAreNotColours() {
        val full = box.copy(left = 0f, top = 0f, right = 1f, bottom = 1f)
        assertEquals(ColourEstimate(ObjectColour.WHITE, false), estimateObjectColour(12, 12, full) { x, y ->
            assertTrue(x in 0..11 && y in 0..11)
            rgb(0xffffff)
        })
        assertNull(estimateObjectColour(100, 100, box) { _, _ -> 0x00ffffff })
    }

    @Test fun smallAndPartiallyClippedRegionsProduceLowConfidenceGuesses() {
        for (region in listOf(box.copy(left = -0.1f), box.copy(right = 1.1f),
            box.copy(right = 0.25f, bottom = 0.25f))) {
            assertEquals(red.copy(lowConfidence = true), estimateObjectColour(100, 100, region) { x, y ->
                assertTrue(x in 0..99 && y in 0..99)
                rgb(0xff0000)
            })
        }
    }

    @Test fun guessesImmediatelyAndImprovesConfidenceWithAgreement() {
        val window = ColourObservationWindow()
        assertEquals(red.copy(lowConfidence = true), window.add(box, geometry, red, 0))
        assertEquals(red.copy(lowConfidence = true), window.add(box, geometry, red, 100))
        assertEquals(red, window.add(box, geometry, red, 200))
    }

    @Test fun smoothsBriefChangesButSwitchesWhenNewColourWins() {
        val window = stableWindow()
        assertEquals(red.copy(lowConfidence = true), window.add(box, geometry, blue, 700))
        assertEquals(red.copy(lowConfidence = true), window.add(box, geometry, blue, 800))
        assertEquals(blue.copy(lowConfidence = true), window.add(box, geometry, blue, 900))
        assertNull(window.add(box, geometry, null, 1400))
        assertEquals(blue.copy(lowConfidence = true), window.add(box, geometry, blue, 1500))
    }

    @Test fun resetOnMissingObjectFrameGapGeometryLabelOrSpatialSelectionChanges() {
        for (change in listOf("clear", "gap", "clock", "geometry", "label", "box")) {
            val window = stableWindow()
            if (change == "clear") window.clear()
            val next = when (change) {
                "label" -> box.copy(label = "cup")
                "box" -> box.copy(left = 0.8f, right = 1f)
                else -> box
            }
            assertEquals(change, blue.copy(lowConfidence = true), window.add(next,
                if (change == "geometry") geometry.copy(rotation = 90) else geometry,
                blue, when (change) { "gap" -> 2000; "clock" -> 500; else -> 700 }))
        }
    }

    @Test fun minorBoxJitterKeepsStableColour() {
        assertEquals(red, stableWindow().add(box.copy(left = 0.21f), geometry, red, 700))
    }

    @Test fun feedbackIncludesColourWithoutSpeakingConfidenceAndDoesNotGuessUnknown() {
        val known = colouredObjectFeedback(box, red)
        assertTrue(known.display.contains("90%"))
        assertEquals("chair detected. Possible colour: red", known.speech)
        assertTrue(known.display.contains("moderate colour confidence"))
        val weak = colouredObjectFeedback(box.copy(confidence = 0.95f), red.copy(lowConfidence = true))
        assertEquals(known.speech, weak.speech)
        assertTrue(weak.display.contains("low colour confidence"))
        val unknown = colouredObjectFeedback(box, null)
        assertEquals("chair detected. Colour unclear", unknown.speech)
        assertTrue(unknown.display.contains("Colour unclear"))
        assertNull(known.obstacle)
    }

    @Test fun everyColourAndUnclearFeedbackTranslateOfflineForKnownObjects() = runBlocking {
        GoogleCloudTranslator.clearCache()
        try {
            for (colour in ObjectColour.entries) {
                val result = GoogleCloudTranslator.translate(colouredObjectFeedback(box, ColourEstimate(colour, true)).speech)
                assertEquals(TranslationSource.LOCAL_DICTIONARY, result.source)
                assertEquals("Setulo se bonoe. 'Mala e ka ba ${colour.sesotho}", result.translatedText)
            }
            val unclear = GoogleCloudTranslator.translate(colouredObjectFeedback(box, null).speech)
            assertEquals(TranslationSource.LOCAL_DICTIONARY, unclear.source)
            assertEquals("Setulo se bonoe. 'Mala ha o hlake", unclear.translatedText)
        } finally {
            GoogleCloudTranslator.clearCache()
        }
    }

    private fun stableWindow() = ColourObservationWindow().apply {
        add(box, geometry, red, 0)
        add(box, geometry, red, 300)
        assertEquals(red, add(box, geometry, red, 600))
    }

    @Test fun alternatingTiesKeepPreviousGuessAndLowConfidencePersistsForMixedSamples() {
        val window = ColourObservationWindow()
        assertEquals(red.copy(lowConfidence = true), window.add(box, geometry, red, 0))
        assertEquals(red.copy(lowConfidence = true), window.add(box, geometry, blue, 100))
        window.clear()
        repeat(8) { index ->
            assertEquals(red.copy(lowConfidence = true),
                window.add(box, geometry, red.copy(lowConfidence = true), index * 100L))
        }
    }

    @Test fun expiredHistoryDoesNotOutvoteCurrentGuess() {
        val window = stableWindow()
        window.add(box, geometry, blue, 1500)
        assertEquals(blue.copy(lowConfidence = true), window.add(box, geometry, blue, 1801))
    }
}
