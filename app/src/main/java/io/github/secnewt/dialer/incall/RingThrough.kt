package io.github.secnewt.dialer.incall

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import io.github.secnewt.dialer.calls.CallPhase
import io.github.secnewt.dialer.calls.RingRules

/**
 * Plays the ringtone itself instead of leaving it to Android, in two cases:
 * - ringing through silent mode for a starred contact or someone calling again (as an alarm,
 *   so it uses the alarm volume and isn't muted by silent mode), and
 * - taking turns with the caller announcement, so the ring pauses while the voice speaks.
 *
 * It stops when the call is answered, declined or stops ringing. Runs on the main thread.
 */
object RingThrough {

    private const val TAG = "RingThrough"
    private const val VIBRATE_WHEN_RINGING = "vibrate_when_ringing"
    private const val OWN_SILENCE_ECHO_MILLIS = 1500L

    private var appContext: Context? = null
    private var usage = AudioAttributes.USAGE_NOTIFICATION_RINGTONE
    private var ringtone: Ringtone? = null
    private var paused = false
    private var vibrating = false

    /** When this app last silenced Android's ringtone, so the echo of that isn't taken as the user's. */
    private var silencedAt = 0L

    private val active get() = appContext != null

    /** Rings through silent mode at alarm volume. */
    fun start(context: Context) {
        if (active) return
        begin(context, AudioAttributes.USAGE_ALARM)
        // Never log who is calling.
        Log.i(TAG, "Ringing through silent mode")
    }

    /**
     * Gets ready for the caller announcement: when the rules allow it, Android's ringtone is
     * silenced and this app rings instead, so [pause] and [resume] can make room for the
     * voice. Returns false when Android keeps ringing and the two simply overlap.
     */
    // silenceRinger is allowed for the default phone app, which takeTurns checks first.
    @SuppressLint("MissingPermission")
    fun takeOver(context: Context): Boolean {
        val audio = context.getSystemService(AudioManager::class.java)
        val telecom = context.getSystemService(TelecomManager::class.java)
        val turns = RingRules.takeTurns(
            isPhoneApp = telecom.defaultDialerPackage == context.packageName,
            callRingingHere = CallManager.calls.value.any { it.phase == CallPhase.RINGING },
            ringerAudible = audio.ringerMode == AudioManager.RINGER_MODE_NORMAL,
            ringingThrough = active,
        )
        if (!turns) return false
        if (active) return true
        try {
            silencedAt = SystemClock.elapsedRealtime()
            telecom.silenceRinger()
        } catch (e: SecurityException) {
            return false
        }
        begin(context, AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
        // Silencing Android's ringtone also stopped its vibration, so vibrate here instead.
        if (vibratesForCalls(context)) vibrate(context)
        Log.i(TAG, "Ringing in turns with the announcement")
        return true
    }

    /** Quiet while the announcement speaks. */
    fun pause() {
        if (!active || paused) return
        paused = true
        ringtone?.stop()
        ringtone = null
    }

    /** Back to ringing after the announcement. */
    fun resume() {
        val context = appContext ?: return
        if (!paused) return
        paused = false
        play(context)
    }

    /**
     * The user silenced the call (volume or power button). Android tells every phone app,
     * including right after [takeOver] silences its ringtone; that echo is ignored and
     * false is returned.
     */
    fun silencedByUser(): Boolean {
        if (SystemClock.elapsedRealtime() - silencedAt < OWN_SILENCE_ECHO_MILLIS) return false
        stop()
        return true
    }

    fun stop() {
        ringtone?.stop()
        ringtone = null
        appContext?.let { if (vibrating) vibrator(it).cancel() }
        vibrating = false
        paused = false
        appContext = null
    }

    private fun begin(context: Context, usage: Int) {
        appContext = context.applicationContext
        this.usage = usage
        paused = false
        play(context.applicationContext)
    }

    private fun play(context: Context) {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val tone = RingtoneManager.getRingtone(context, uri) ?: return
        tone.audioAttributes = AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        tone.isLooping = true
        try {
            tone.play()
            ringtone = tone
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't play the ringtone")
        }
    }

    /** The "Vibrate for calls" switch in Android's sound settings. */
    private fun vibratesForCalls(context: Context): Boolean =
        Settings.System.getInt(context.contentResolver, VIBRATE_WHEN_RINGING, 0) == 1

    @Suppress("DEPRECATION") // The AudioAttributes form still works and covers Android 11 and 12.
    private fun vibrate(context: Context) {
        val vibrator = vibrator(context)
        if (!vibrator.hasVibrator()) return
        // One second on, one second off, until the call stops ringing.
        val pattern = VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0)
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build()
        vibrator.vibrate(pattern, attributes)
        vibrating = true
    }

    private fun vibrator(context: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
}
