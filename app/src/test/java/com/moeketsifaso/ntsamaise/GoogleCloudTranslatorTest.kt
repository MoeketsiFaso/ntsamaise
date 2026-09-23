package com.moeketsifaso.ntsamaise

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GoogleCloudTranslatorTest {

    @Before
    fun setUp() {
        GoogleCloudTranslator.clearCache()
        GoogleCloudTranslator.customApiKey = "TEST_API_KEY"
    }

    @After
    fun tearDown() {
        GoogleCloudTranslator.clearCache()
        GoogleCloudTranslator.customApiKey = null
        GoogleCloudTranslator.httpTransport = DefaultHttpTransport()
    }

    @Test
    fun localDictionaryTranslatesKnownPromptsOffline() = runBlocking {
        GoogleCloudTranslator.customApiKey = null // No API key
        val result = GoogleCloudTranslator.translate("person detected")
        assertEquals(TranslationSource.LOCAL_DICTIONARY, result.source)
        assertEquals("Motho o bonoe", result.translatedText)
    }

    @Test
    fun localDictionaryMatchesDynamicCurrencyPatterns() = runBlocking {
        GoogleCloudTranslator.customApiKey = null
        val prompt = "Possible 50 maloti, based on printed text. Check the note before using this result."
        val result = GoogleCloudTranslator.translate(prompt)
        assertEquals(TranslationSource.LOCAL_DICTIONARY, result.source)
        assertTrue(result.translatedText.contains("maloti a 50"))
    }

    @Test
    fun cloudTranslationParsesSuccessResponseAndDecodesHtml() = runBlocking {
        var requestedUrl: String? = null
        var requestedBody: String? = null

        GoogleCloudTranslator.httpTransport = object : HttpTransport {
            override fun post(url: String, jsonBody: String): Pair<Int, String> {
                requestedUrl = url
                requestedBody = jsonBody
                val response = """
                    {
                      "data": {
                        "translations": [
                          {
                            "translatedText": "Lefat&#39;se le &quot;Lumela&quot; &amp; ba bang"
                          }
                        ]
                      }
                    }
                """.trimIndent()
                return Pair(200, response)
            }
        }

        val result = GoogleCloudTranslator.translate("Hello world and others")
        assertEquals(TranslationSource.CLOUD, result.source)
        assertEquals("Lefat'se le \"Lumela\" & ba bang", result.translatedText)
        assertTrue(requestedUrl?.contains("key=TEST_API_KEY") == true)
        assertTrue(requestedBody?.contains("\"target\":\"st\"") == true)
        assertTrue(requestedBody?.contains("\"source\":\"en\"") == true)
    }

    @Test
    fun repeatedTranslationsUseCacheWithoutHttpCalls() = runBlocking {
        var httpCalls = 0
        GoogleCloudTranslator.httpTransport = object : HttpTransport {
            override fun post(url: String, jsonBody: String): Pair<Int, String> {
                httpCalls++
                return Pair(200, """{"data":{"translations":[{"translatedText":"Palo"}]}}""")
            }
        }

        val first = GoogleCloudTranslator.translate("Custom arbitrary sentence")
        assertEquals(TranslationSource.CLOUD, first.source)
        assertEquals(1, httpCalls)

        val second = GoogleCloudTranslator.translate("Custom arbitrary sentence")
        assertEquals(TranslationSource.CACHE, second.source)
        assertEquals("Palo", second.translatedText)
        assertEquals(1, httpCalls) // No second HTTP call
    }

    @Test
    fun missingApiKeyFallsBackSafely() = runBlocking {
        GoogleCloudTranslator.customApiKey = ""
        val result = GoogleCloudTranslator.translate("An unmapped text without API key")
        assertEquals(TranslationSource.FALLBACK, result.source)
        assertEquals("An unmapped text without API key", result.translatedText)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("API key not set"))
    }

    @Test
    fun httpErrorFallsBackSafelyWithoutThrowing() = runBlocking {
        GoogleCloudTranslator.httpTransport = object : HttpTransport {
            override fun post(url: String, jsonBody: String): Pair<Int, String> {
                return Pair(403, """{"error":{"code":403,"message":"Quota exceeded"}}""")
            }
        }

        val result = GoogleCloudTranslator.translate("Some text that causes an error")
        assertEquals(TranslationSource.FALLBACK, result.source)
        assertEquals("Some text that causes an error", result.translatedText)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Quota exceeded"))
    }
}
