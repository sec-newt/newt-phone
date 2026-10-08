package io.github.secnewt.dialer.incall

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.util.Log

/**
 * Plays the ringtone for a caller who should be heard even though the phone is on silent
 * (a starred contact, or someone calling again). It plays as an alarm, so it uses the alarm
 * volume and isn't muted by silent mode. Runs on the main thread.
 */
object RingThrough {

    private const val TAG = "RingThrough"
    private var ringtone: Ringtone? = null

    fun start(context: Context) {
        if (ringtone?.isPlaying == true) return
        val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val tone = RingtoneManager.getRingtone(context.applicationContext, uri) ?: return
        tone.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        tone.isLooping = true
        try {
            tone.play()
            ringtone = tone
            // Never log who is calling.
            Log.i(TAG, "Ringing through silent mode")
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't play the ringtone")
        }
    }

    fun stop() {
        ringtone?.stop()
        ringtone = null
    }
}
