package com.moeketsifaso.ntsamaise

import org.junit.Assert.*
import org.junit.Test

class TextReadingTest {
    @Test
    fun longPassageIsPreservedAndEveryChunkFits() {
        val text = "A printed paragraph with punctuation.\nSecond line.\n\n".repeat(250)
        val chunks = speechChunks(text, 3999)
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 3999 })
        assertEquals(text, chunks.joinToString(""))
    }

    @Test
    fun splitsAtWordsWhenPossible() {
        assertEquals(listOf("Read this ", "text aloud"), speechChunks("Read this text aloud", 12))
    }

    @Test
    fun unbrokenTextAndUnicodeAreNotLostOrSplitInsideSurrogatePairs() {
        val text = "abc\uD83D\uDE00".repeat(10)
        val chunks = speechChunks(text, 4)
        assertEquals(text, chunks.joinToString(""))
        assertTrue(chunks.all { it.length <= 4 })
        assertTrue(chunks.none { it.first().isLowSurrogate() || it.last().isHighSurrogate() })
    }

    @Test
    fun blankTextDoesNotCreateSpeech() {
        assertTrue(speechChunks(" \n\t", 100).isEmpty())
    }

    @Test
    fun eachCaptureProcessesOnlyOneFrameAndBlocksDuplicateRequests() {
        val requests = TextCaptureRequests()
        val id = requests.request()!!
        assertNull(requests.request())
        assertEquals(id, requests.take())
        assertNull(requests.take())
        assertNull(requests.request())
        assertTrue(requests.complete(id))
        assertFalse(requests.complete(id))
        assertNotNull(requests.request())
    }

    @Test
    fun cancelledResultCannotReplaceANewerCapture() {
        val requests = TextCaptureRequests()
        val oldId = requests.request()!!
        requests.take()
        requests.cancel()
        val newId = requests.request()!!
        assertFalse(requests.complete(oldId))
        assertEquals(newId, requests.take())
        assertTrue(requests.complete(newId))
    }

    @Test
    fun cancelBeforeAFrameArrivesDiscardsTheRequest() {
        val requests = TextCaptureRequests()
        val id = requests.request()!!
        requests.cancel()
        assertNull(requests.take())
        assertFalse(requests.complete(id))
    }
}
