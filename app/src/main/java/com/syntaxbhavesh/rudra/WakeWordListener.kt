package com.syntaxbhavesh.rudra

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * App-screen-only wake word listener. It re-arms short recognition sessions while enabled.
 * Android's configured speech recognition provider may process the audio.
 */
class WakeWordListener(
    private val activity: Activity,
    private val onWakeWord: () -> Unit,
    private val onStatus: (String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    @Volatile private var running = false
    private var triggered = false

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            onStatus("Speech recognition unavailable")
            return
        }
        stop()
        running = true
        triggered = false
        onStatus("Listening for “Hey RUDRA”")
        arm()
    }

    private fun arm() {
        if (!running || triggered || activity.isFinishing || activity.isDestroyed) return
        handler.postDelayed({
            if (!running || triggered || activity.isFinishing || activity.isDestroyed) return@postDelayed
            try {
                val sr = recognizer ?: SpeechRecognizer.createSpeechRecognizer(activity).also { recognizer = it }
                sr.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: android.os.Bundle?) { onStatus("Listening for “Hey RUDRA”") }
                    override fun onBeginningOfSpeech() { onStatus("Microphone active • listening") }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) { stopRecognizer(); arm() }
                    override fun onResults(results: android.os.Bundle?) {
                        val candidates = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                        if (candidates.any(::isWakePhrase)) {
                            triggered = true
                            running = false
                            stopRecognizer()
                            onStatus("Wake word detected")
                            onWakeWord()
                        } else {
                            arm()
                        }
                    }
                    override fun onPartialResults(partialResults: android.os.Bundle?) {
                        val candidates = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                        if (candidates.any(::isWakePhrase)) {
                            triggered = true
                            running = false
                            stopRecognizer()
                            onStatus("Wake word detected")
                            onWakeWord()
                        }
                    }
                    override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                })
                sr.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                })
            } catch (_: Exception) {
                onStatus("Wake-word service unavailable")
                stop()
            }
        }, 500)
    }

    private fun isWakePhrase(value: String): Boolean = RudraCommandRules.isWakePhrase(value)

    private fun stopRecognizer() {
        recognizer?.let {
            runCatching { it.cancel() }
            runCatching { it.destroy() }
        }
        recognizer = null
    }

    fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
        stopRecognizer()
    }
}
