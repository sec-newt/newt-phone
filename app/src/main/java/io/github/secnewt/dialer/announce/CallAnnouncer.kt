package io.github.secnewt.dialer.announce

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log

/**
 * Speaks the caller announcement with the phone's text-to-speech voice, once or over and over
 * while the phone rings. Only one announcement plays at a time; a new one or [stop] cuts the
 * old one off. Everything runs on the main thread, so no locking is needed.
 */
object CallAnnouncer {

    private const val TAG = "CallAnnouncer"
    private const val UTTERANCE_ID = "caller"

    /** Give up if the voice hasn't finished in time, so a broadcast never hangs. */
    private const val TIMEOUT_MS = 9_000L

    /** The pause between repeats, so the ringtone is heard in between. */
    private const val REPEAT_GAP_MS = 2_000L

    /** Stop repeating after this many times even if nobody said to (about a minute). */
    private const val MAX_TIMES = 12

    private val main = Handler(Looper.getMainLooper())
    private var session = 0
    private var tts: TextToSpeech? = null
    private var timeout: Runnable? = null
    private var finish: ((Boolean) -> Unit)? = null

    /**
     * Speaks [text], repeating it after a short pause when [repeat] is true until [stop] is
     * called. [onDone] is called exactly once: with true after the first time it's spoken, or
     * with false if no voice is installed, it failed, or it was cut off before that.
     */
    fun speak(context: Context, text: String, repeat: Boolean = false, onDone: (spoke: Boolean) -> Unit = {}) {
        main.post {
            stopNow()
            val id = ++session
            var reported = false
            val report: (Boolean) -> Unit = { spoke ->
                if (!reported) {
                    reported = true
                    onDone(spoke)
                }
            }
            finish = report
            timeout = Runnable {
                report(false)
                if (session == id) release()
            }.also { main.postDelayed(it, TIMEOUT_MS) }

            tts = TextToSpeech(context.applicationContext) { status ->
                val engine = tts
                if (session != id) return@TextToSpeech
                if (status != TextToSpeech.SUCCESS || engine == null) {
                    Log.w(TAG, "No text-to-speech voice is available")
                    report(false)
                    release()
                    return@TextToSpeech
                }
                // Play on the ringtone stream so it follows the ring volume.
                engine.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                var times = 0
                val say = {
                    if (session == id && engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID) != TextToSpeech.SUCCESS) {
                        report(false)
                        release()
                    }
                }
                val failed = {
                    if (session == id) {
                        report(false)
                        release()
                    }
                }
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) {
                        main.post {
                            if (session != id) return@post
                            times++
                            timeout?.let { main.removeCallbacks(it) }
                            timeout = null
                            report(true)
                            if (repeat && times < MAX_TIMES) main.postDelayed(say, REPEAT_GAP_MS) else release()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        main.post(failed)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        main.post(failed)
                    }
                })
                say()
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
        // A new session number makes any repeat that's already scheduled do nothing.
        session++
        timeout?.let { main.removeCallbacks(it) }
        timeout = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        finish = null
    }
}
