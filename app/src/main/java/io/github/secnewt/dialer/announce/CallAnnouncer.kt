package io.github.secnewt.dialer.announce

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log

/**
 * Speaks one announcement with the phone's text-to-speech voice.
 * Only one announcement plays at a time; a new one or [stop] cuts the old one off.
 * Everything runs on the main thread, so no locking is needed.
 */
object CallAnnouncer {

    private const val TAG = "CallAnnouncer"
    private const val UTTERANCE_ID = "caller"

    /** Give up if the voice has not finished in time, so a broadcast never hangs. */
    private const val TIMEOUT_MS = 9_000L

    private val main = Handler(Looper.getMainLooper())
    private var session = 0
    private var tts: TextToSpeech? = null
    private var timeout: Runnable? = null
    private var finish: ((Boolean) -> Unit)? = null

    /**
     * Speaks [text], then calls [onDone] with true, or with false if no voice is
     * installed, it failed, or it was cut off. [onDone] is always called exactly once.
     */
    fun speak(context: Context, text: String, onDone: (spoke: Boolean) -> Unit = {}) {
        main.post {
            stopNow()
            val id = ++session
            var completed = false
            val done: (Boolean) -> Unit = { spoke ->
                if (!completed) {
                    completed = true
                    if (session == id) release()
                    onDone(spoke)
                }
            }
            finish = done
            timeout = Runnable { done(false) }.also { main.postDelayed(it, TIMEOUT_MS) }

            tts = TextToSpeech(context.applicationContext) { status ->
                val engine = tts
                if (session != id) return@TextToSpeech
                if (status != TextToSpeech.SUCCESS || engine == null) {
                    Log.w(TAG, "No text-to-speech voice is available")
                    done(false)
                    return@TextToSpeech
                }
                // Play on the ringtone stream so it follows the ring volume.
                engine.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) {
                        main.post { done(true) }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        main.post { done(false) }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        main.post { done(false) }
                    }
                })
                if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID) != TextToSpeech.SUCCESS) {
                    done(false)
                }
            }
        }
    }

    /** Stops any announcement, for example when the call is answered or ends. */
    fun stop() {
        main.post { stopNow() }
    }

    private fun stopNow() {
        finish?.invoke(false)
        release()
    }

    private fun release() {
        timeout?.let { main.removeCallbacks(it) }
        timeout = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        finish = null
    }
}
