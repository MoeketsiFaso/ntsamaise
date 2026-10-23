package com.moeketsifaso.ntsamaise

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AppLanguageTest {
    @Test fun savedLanguageCodesAreExplicitAndUnknownCodesRequireChoice() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.SESOTHO, AppLanguage.fromCode("st"))
        assertNull(AppLanguage.fromCode(null))
        assertNull(AppLanguage.fromCode("fr"))
    }

    @Test fun englishResultsNeverCallTranslationEvenWhenSesothoIsCached() = runBlocking {
        AppLanguage.SESOTHO.output("chair detected")
        val previous = GoogleCloudTranslator.httpTransport
        GoogleCloudTranslator.httpTransport = object : HttpTransport {
            override fun post(url: String, jsonBody: String): Pair<Int, String> =
                error("English must not request a translation")
        }
        try {
            for (text in listOf("chair detected", "Arbitrary printed passage", "Possible obstacle ahead")) {
                val result = AppLanguage.ENGLISH.output(text)
                assertEquals(text, result.translatedText)
                assertEquals(TranslationSource.ORIGINAL, result.source)
            }
        } finally { GoogleCloudTranslator.httpTransport = previous }
    }

    @Test fun sesothoGuidanceAndErrorsWorkOffline() = runBlocking {
        for (text in listOf("Read Text", "Camera unavailable. Reopen this screen to retry.",
            "No text found. Improve the lighting, move closer and try again.")) {
            val result = AppLanguage.SESOTHO.output(text)
            assertEquals(AppLanguage.SESOTHO.ui(text), result.translatedText)
            assertNotEquals(text, result.translatedText)
            assertNotEquals(TranslationSource.FALLBACK, result.source)
        }
    }

    @Test fun allInterfaceEntriesHaveBothLanguages() {
        sesothoUi.forEach { (english, sesotho) ->
            assertTrue(sesotho.isNotBlank())
            assertEquals(english, AppLanguage.ENGLISH.ui(english))
            assertEquals(sesotho, AppLanguage.SESOTHO.ui(english))
        }
    }
}
