package com.moeketsifaso.ntsamaise

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

/** Stable, rate-limited announcements using the shared speech lifecycle. */
@Composable
fun rememberSpokenDetection(result: String?): String? {
    val speech = rememberSpeechOutput()
    val gate = remember { AnnouncementGate() }

    LaunchedEffect(speech.resumed) {
        if (!speech.resumed) gate.reset()
    }
    LaunchedEffect(result, speech.ready, speech.resumed) {
        if (result == null) speech.stop()
        if (result != null && speech.ready && speech.resumed) {
            delay(800)
            while (true) {
                val now = SystemClock.elapsedRealtime()
                if (!speech.isSpeaking && gate.canAnnounce(result, now)) {
                    if (speech.speak(result)) gate.record(result, now) else break
                }
                delay(250)
            }
        }
    }
    return speech.error
}

/** Separate intervals let new results through without repeatedly reading an unchanged scene. */
internal class AnnouncementGate {
    private var lastResult: String? = null
    private var lastTime = 0L

    fun canAnnounce(result: String, now: Long): Boolean {
        if (lastResult == null) return true
        val interval = if (result == lastResult) 8_000L else 3_000L
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
