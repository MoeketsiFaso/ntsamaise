package com.moeketsifaso.ntsamaise

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.util.Locale

/** Owns speech for the scan screen; detection updates never build an audio queue. */
@Composable
fun rememberSpokenDetection(result: String?): String? {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resumed by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    val gate = remember { AnnouncementGate() }

    DisposableEffect(context) {
        var disposed = false
        val handler = Handler(Looper.getMainLooper())
        // Posting also handles engines that report initialization before construction returns.
        val tts = TextToSpeech(context) { status ->
            handler.post {
                if (!disposed) {
                    val current = engine
                    if (status == TextToSpeech.SUCCESS && current != null) {
                        val language = current.setLanguage(Locale.ENGLISH)
                        if (language >= TextToSpeech.LANG_AVAILABLE) {
                            current.voices?.firstOrNull {
                                it.locale.language == Locale.ENGLISH.language &&
                                    !it.isNetworkConnectionRequired &&
                                    !it.features.orEmpty().contains("notInstalled")
                            }?.let { current.voice = it }
                            ready = true
                        } else {
                            error = "English speech unavailable. Install an English voice in your phone's text-to-speech settings."
                        }
                    } else {
                        error = "Speech unavailable. Check your phone's text-to-speech settings."
                    }
                }
            }
        }
        engine = tts
        onDispose {
            disposed = true
            ready = false
            engine = null
            tts.stop()
            tts.shutdown()
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (!resumed) {
                engine?.stop()
                gate.reset()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(result, ready, resumed) {
        // A new result must persist briefly before it is read aloud.
        if (result != null && ready && resumed) {
            delay(800)
            while (true) {
                val current = engine
                val now = SystemClock.elapsedRealtime()
                if (current != null && !current.isSpeaking && gate.canAnnounce(result, now)) {
                    val status = current.speak(result, TextToSpeech.QUEUE_FLUSH, null, "detection")
                    if (status == TextToSpeech.SUCCESS) {
                        gate.record(result, now)
                        error = null
                    } else {
                        error = "Could not speak the result. Check your phone's text-to-speech settings."
                        break
                    }
                }
                delay(250)
            }
        }
    }
    return error
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
