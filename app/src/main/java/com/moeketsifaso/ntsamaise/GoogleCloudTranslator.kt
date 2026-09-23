package com.moeketsifaso.ntsamaise

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

enum class TranslationSource {
    CLOUD,
    CACHE,
    LOCAL_DICTIONARY,
    FALLBACK
}

data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val source: TranslationSource,
    val error: String? = null
)

interface HttpTransport {
    fun post(url: String, jsonBody: String): Pair<Int, String>
}

internal class DefaultHttpTransport : HttpTransport {
    override fun post(url: String, jsonBody: String): Pair<Int, String> {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(jsonBody)
            writer.flush()
        }
        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val responseBody = stream?.use {
            BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText()
        }.orEmpty()
        connection.disconnect()
        return Pair(statusCode, responseBody)
    }
}

/**
 * Cloud Google Translate API client with in-memory caching and offline dictionary fallbacks for Sesotho.
 */
object GoogleCloudTranslator {
    @Volatile
    var customApiKey: String? = null

    internal var httpTransport: HttpTransport = DefaultHttpTransport()

    private val cache = ConcurrentHashMap<String, String>()

    /** Pre-translated app guidance and common object names for immediate, offline response. */
    private val localDictionary = mapOf(
        // Object detection & status
        "Looking for objects..." to "E batla lintho...",
        "No object recognized" to "Ha ho ntho e lemohuoeng",
        "person detected" to "Motho o bonoe",
        "chair detected" to "Setulo se bonoe",
        "bottle detected" to "Botlolo e bonoe",
        "cup detected" to "Kopi e bonoe",
        "book detected" to "Buka e bonoe",
        "cell phone detected" to "Fono e bonoe",
        "car detected" to "Koloi e bonoe",
        "cat detected" to "Katsana e bonoe",
        "dog detected" to "Ntja e bonoe",
        "laptop detected" to "Laptop e bonoe",
        "tv detected" to "Thelevishene e bonoe",
        "door detected" to "Monako o bonoe",
        "table detected" to "Tafole e bonoe",
        "clock detected" to "Oache e bonoe",
        "bag detected" to "Mokotla o bonoe",
        "backpack detected" to "Mokotla o bonoe",
        "keyboard detected" to "Khiboto e bonoe",
        "mouse detected" to "Kokoana-thoko e bonoe",

        // Currency guidance
        "No text found. Hold one note flat in good light and scan again." to
            "Ha ho mongolo o fumanoeng. Beha chelete e le 'ngoe hantle leseling 'me u hlahlobe hape.",
        "Could not identify Lesotho banknote text. Show the side with the bank name and scan again." to
            "Ha rea khona ho bona mongolo oa chelete ea Lesotho. Bontša lehlakore le nang le lebitso la banka 'me u hlahlobe hape.",
        "Conflicting currency or denomination text. Show only one note and scan again." to
            "Mongolo oa chelete oa hanyetsana. Bontša chelete e le 'ngoe feela 'me u hlahlobe hape.",
        "Could not read the denomination clearly. Show the whole note, move closer and scan again." to
            "Ha rea khona ho bala boleng ba chelete hantle. Bontša chelete eohle, atametsa 'me u hlahlobe hape.",

        // Read text guidance
        "Point the camera at printed text, then tap Capture and read." to
            "Lebisa khamera mongolong o hatisitsoeng, ebe u tobetsa Tšoara 'me u bale.",
        "Recognizing text. Hold the camera steady." to
            "E bala mongolo. Tšoara khamera e sa tsitsinyehe.",
        "Text captured. Tap Read again to repeat, or capture another passage." to
            "Mongolo o tšoeroe. Tobetsa Bala hape ho pheta, kapa u tšoare mongolo o mong.",
        "No text found. Improve the lighting, move closer and try again." to
            "Ha ho mongolo o fumanoeng. Ntlafatsa leseli, atametsa 'me u leke hape.",
        "Capture cancelled. Tap Capture and read to try again." to
            "Ho tšoara ho hlakotsoe. Tobetsa Tšoara 'me u bale ho leka hape."
    )

    fun getEffectiveApiKey(): String {
        return customApiKey?.takeIf { it.isNotBlank() }
            ?: runCatching { BuildConfig.GOOGLE_TRANSLATE_API_KEY }.getOrDefault("")
    }

    suspend fun translate(
        text: String,
        sourceLang: String = "en",
        targetLang: String = "st"
    ): TranslationResult = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return@withContext TranslationResult(text, text, TranslationSource.FALLBACK)
        }

        // 1. Check in-memory cache
        val cached = cache[trimmed]
        if (cached != null) {
            return@withContext TranslationResult(trimmed, cached, TranslationSource.CACHE)
        }

        // 2. Check local offline dictionary
        val local = matchLocalDictionary(trimmed)
        if (local != null) {
            cache[trimmed] = local
            return@withContext TranslationResult(trimmed, local, TranslationSource.LOCAL_DICTIONARY)
        }

        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext TranslationResult(
                trimmed,
                trimmed,
                TranslationSource.FALLBACK,
                "Google Translate API key not set. Using original text."
            )
        }

        // 3. Query Cloud Google Translate API
        try {
            val endpoint = "https://translation.googleapis.com/language/translate/v2?key=$apiKey"
            val escaped = escapeJson(trimmed)
            val requestJson = "{\"q\":[\"$escaped\"],\"source\":\"$sourceLang\",\"target\":\"$targetLang\",\"format\":\"text\"}"

            val (code, responseBody) = httpTransport.post(endpoint, requestJson)
            if (code in 200..299) {
                val translatedText = extractTranslatedText(responseBody)
                if (translatedText != null) {
                    val unescaped = unescapeHtml(translatedText)
                    cache[trimmed] = unescaped
                    return@withContext TranslationResult(trimmed, unescaped, TranslationSource.CLOUD)
                }
            } else {
                val errorMessage = extractErrorMessage(responseBody) ?: "HTTP $code"
                return@withContext TranslationResult(
                    trimmed,
                    trimmed,
                    TranslationSource.FALLBACK,
                    "Translation error: $errorMessage"
                )
            }
        } catch (e: Exception) {
            return@withContext TranslationResult(
                trimmed,
                trimmed,
                TranslationSource.FALLBACK,
                "Translation failed: ${e.message ?: "network error"}"
            )
        }

        return@withContext TranslationResult(trimmed, trimmed, TranslationSource.FALLBACK)
    }

    private fun matchLocalDictionary(text: String): String? {
        localDictionary[text]?.let { return it }

        // Dynamic currency denomination match
        val currencyMatch = Regex("Possible (\\d+) maloti, based on printed text\\. Check the note before using this result\\.").matchEntire(text)
        if (currencyMatch != null) {
            val amount = currencyMatch.groupValues[1]
            return "E ka 'na eaba ke maloti a $amount, ho latela mongolo o hatisitsoeng. Hlahloba chelete pele u sebelisa sephetho sena."
        }

        // Dynamic object detection label
        if (text.endsWith(" detected")) {
            val obj = text.removeSuffix(" detected")
            val translatedObj = localDictionary["$obj detected"]
            if (translatedObj != null) return translatedObj
        }

        return null
    }

    internal fun escapeJson(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\b' -> sb.append("\\b")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    internal fun unescapeJson(text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (ch == '\\' && i + 1 < text.length) {
                when (text[i + 1]) {
                    '"' -> { sb.append('"'); i += 2; continue }
                    '\\' -> { sb.append('\\'); i += 2; continue }
                    '/' -> { sb.append('/'); i += 2; continue }
                    'n' -> { sb.append('\n'); i += 2; continue }
                    'r' -> { sb.append('\r'); i += 2; continue }
                    't' -> { sb.append('\t'); i += 2; continue }
                    'u' -> {
                        if (i + 5 < text.length) {
                            val hex = text.substring(i + 2, i + 6)
                            val code = hex.toIntOrNull(16)
                            if (code != null) {
                                sb.append(code.toChar())
                                i += 6
                                continue
                            }
                        }
                    }
                }
            }
            sb.append(ch)
            i++
        }
        return sb.toString()
    }

    internal fun extractTranslatedText(json: String): String? {
        val regex = Regex("\"translatedText\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"")
        return regex.find(json)?.groupValues?.get(1)?.let { unescapeJson(it) }
    }

    internal fun extractErrorMessage(json: String): String? {
        val regex = Regex("\"message\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"")
        return regex.find(json)?.groupValues?.get(1)?.let { unescapeJson(it) }
    }

    internal fun unescapeHtml(text: String): String {
        return text
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#34;", "\"")
            .replace("&apos;", "'")
    }

    fun clearCache() {
        cache.clear()
    }
}
