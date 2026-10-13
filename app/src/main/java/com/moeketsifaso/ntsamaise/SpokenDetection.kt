package com.moeketsifaso.ntsamaise

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

/** Stable, rate-limited announcements using the shared speech lifecycle. */
@Composable
fun rememberSpokenDetection(
    result: String?, stabilityMillis: Long = 800,
    changedIntervalMillis: Long = 3_000, repeatIntervalMillis: Long = 8_000
): String? {
    val language = LocalAppLanguage.current
    val speech = rememberSpeechOutput()
    val gate = remember(language, changedIntervalMillis, repeatIntervalMillis) {
        AnnouncementGate(changedIntervalMillis, repeatIntervalMillis)
    }

    LaunchedEffect(speech.resumed) {
        if (!speech.resumed) gate.reset()
    }
    LaunchedEffect(language, result, speech.ready, speech.resumed, stabilityMillis, gate) {
        if (result == null) speech.stop()
        if (result != null && speech.ready && speech.resumed) {
            delay(stabilityMillis)
            while (true) {
                val now = SystemClock.elapsedRealtime()
                if (!speech.isSpeaking && gate.canAnnounce(result, now)) {
                    val translation = language.output(result)
                    val textToSpeak = translation.translatedText
                    if (speech.speak(textToSpeak)) gate.record(result, now) else break
                }
                delay(250)
            }
        }
    }
    return speech.error
}

/** Separate intervals let new results through without repeatedly reading an unchanged scene. */
internal class AnnouncementGate(
    private val changedIntervalMillis: Long = 3_000,
    private val repeatIntervalMillis: Long = 8_000
) {
    private var lastResult: String? = null
    private var lastTime = 0L

    fun canAnnounce(result: String, now: Long): Boolean {
        if (lastResult == null) return true
        val interval = if (result == lastResult) repeatIntervalMillis else changedIntervalMillis
        return now - lastTime >= interval
    }

    fun record(result: String, now: Long) {
        lastResult = result
        lastTime = now
    }

    fun reset() {
        lastResult = null
        lastTime = 0L
    }
}
