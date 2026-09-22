package com.moeketsifaso.ntsamaise

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

/** Main-thread speech controls shared by object announcements and captured passages. */
internal class SpeechOutput {
    internal var engine: TextToSpeech? = null
    var ready by mutableStateOf(false)
        internal set
    var resumed by mutableStateOf(false)
        internal set
    var isSpeaking by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        internal set
    private var generation = 0L
    private var lastUtterance: String? = null

    fun speak(text: String): Boolean {
        val current = engine ?: return false
        if (!ready || !resumed) return false
        val chunks = speechChunks(text, TextToSpeech.getMaxSpeechInputLength() - 1)
        if (chunks.isEmpty()) return false
        generation++
        lastUtterance = "$generation:${chunks.lastIndex}"
        isSpeaking = true
        error = null
        chunks.forEachIndexed { index, chunk ->
            val status = current.speak(
                chunk, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null, "$generation:$index"
            )
            if (status != TextToSpeech.SUCCESS) {
                stop()
                error = "Could not read aloud. Check your phone's text-to-speech settings."
                return false
            }
        }
        return true
    }

    fun stop() {
        generation++
        lastUtterance = null
        isSpeaking = false
        engine?.stop()
    }

    internal fun utteranceStopped(id: String?) {
        if (id != null && id.startsWith("$generation:") && lastUtterance != null) stop()
    }

    internal fun utteranceFinished(id: String?, failed: Boolean) {
        if (id == null || !id.startsWith("$generation:") || lastUtterance == null) return
        if (failed) {
            stop()
            error = "Could not read aloud. Check your phone's text-to-speech settings."
        } else if (id == lastUtterance) {
            lastUtterance = null
            isSpeaking = false
        }
    }
}

@Composable
internal fun rememberSpeechOutput(): SpeechOutput {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val speech = remember { SpeechOutput() }

    DisposableEffect(context) {
        var disposed = false
        val handler = Handler(Looper.getMainLooper())
        val tts = TextToSpeech(context) { status ->
            // Handles engines that report initialization before construction returns.
            handler.post {
                if (!disposed) {
                    val current = speech.engine
                    if (status == TextToSpeech.SUCCESS && current != null) {
                        if (current.setLanguage(Locale.ENGLISH) >= TextToSpeech.LANG_AVAILABLE) {
                            current.voices?.firstOrNull {
                                it.locale.language == Locale.ENGLISH.language &&
                                    !it.isNetworkConnectionRequired &&
                                    !it.features.orEmpty().contains("notInstalled")
                            }?.let { current.voice = it }
                            speech.ready = true
                        } else {
                            speech.error = "English speech unavailable. Install an English voice in your phone's text-to-speech settings."
                        }
                    } else {
                        speech.error = "Speech unavailable. Check your phone's text-to-speech settings."
                    }
                }
            }
        }
        speech.engine = tts
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                handler.post { if (!disposed) speech.utteranceStopped(utteranceId) }
            }
            override fun onDone(utteranceId: String?) {
                handler.post { if (!disposed) speech.utteranceFinished(utteranceId, false) }
            }
            @Deprecated("Required by the Android TTS listener API")
            override fun onError(utteranceId: String?) {
                handler.post { if (!disposed) speech.utteranceFinished(utteranceId, true) }
            }
            override fun onError(utteranceId: String?, errorCode: Int) {
                handler.post { if (!disposed) speech.utteranceFinished(utteranceId, true) }
            }
        })
        onDispose {
            disposed = true
            speech.ready = false
            speech.stop()
            speech.engine = null
            tts.shutdown()
        }
    }

    DisposableEffect(lifecycle) {
        speech.resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        val observer = LifecycleEventObserver { _, _ ->
            speech.resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (!speech.resumed) speech.stop()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            speech.stop()
        }
    }
    return speech
}
