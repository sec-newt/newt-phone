package io.github.secnewt.dialer.incall

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioManager
import android.provider.CallLog
import android.telecom.Call
import android.telecom.TelecomManager
import android.telecom.CallAudioState
import android.telecom.InCallService
import io.github.secnewt.dialer.announce.CallAnnouncer
import io.github.secnewt.dialer.calls.CallPhase
import io.github.secnewt.dialer.calls.RingAction
import io.github.secnewt.dialer.calls.RingRules
import io.github.secnewt.dialer.calls.RingSituation
import io.github.secnewt.dialer.screening.SpamSettingsStore
import java.time.LocalTime
import io.github.secnewt.dialer.calls.LiveCalls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android hands every phone call to this service once the app is the default phone app.
 * It keeps [CallManager] up to date, opens the call screen and shows the call notification.
 * Android still plays the ringtone itself.
 */
class DialerInCallService : InCallService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var notifier: CallNotifier

    override fun onCreate() {
        super.onCreate()
        notifier = CallNotifier(this)
        CallManager.service = this
        // The ring-through ringtone stops as soon as nothing is ringing any more.
        scope.launch {
            CallManager.calls.collect { calls ->
                if (calls.none { it.phase == CallPhase.RINGING }) RingThrough.stop()
            }
        }
        scope.launch {
            combine(CallManager.calls, CallManager.screenShowing) { calls, showing -> calls to showing }
                .collectLatest { (calls, showing) ->
                    // Give the call screen a moment to open, so the banner doesn't pop up over it.
                    val ringing = LiveCalls.primary(calls)?.phase == CallPhase.RINGING
                    if (ringing && !showing) delay(BANNER_DELAY_MILLIS)
                    notifier.update(calls, screenShowing = showing)
                }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val id = CallManager.add(call)
        val number = CallManager.numberOf(call)
        val incoming = CallManager.calls.value.firstOrNull { it.id == id }?.phase == CallPhase.RINGING
        scope.launch {
            val (info, earlierCalls) = withContext(Dispatchers.IO) {
                CallerLookup(this@DialerInCallService).lookup(number) to
                    if (incoming) recentCalls() else emptyList()
            }
            CallManager.setCallerInfo(id, info)
            if (incoming) {
                applyRingRules(info, RingRules.isRepeat(number, System.currentTimeMillis(), earlierCalls))
                CallManager.rememberIncoming(number)
            }
        }
        // Allowed from here: Android lets a bound in-call service open its call screen.
        startActivity(InCallActivity.intent(this))
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.remove(call)
    }

    @Deprecated("Replaced by onCallEndpointChanged on Android 14; still called there.")
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        @Suppress("DEPRECATION")
        super.onCallAudioStateChanged(audioState)
        CallManager.updateAudio(audioState)
    }

    /**
     * Starred contacts and repeat callers ring even on silent; during quiet hours everyone
     * else rings silently (the call still shows on screen).
     */
    private suspend fun applyRingRules(info: CallerInfo, repeatCaller: Boolean) {
        val settings = SpamSettingsStore(this).ringSettings()
        val now = LocalTime.now()
        val situation = RingSituation(
            ringerAudible = getSystemService(AudioManager::class.java).ringerMode == AudioManager.RINGER_MODE_NORMAL,
            starred = info.starred,
            repeatCaller = repeatCaller,
            flagged = info.spamLabel != null,
            minutesAfterMidnight = now.hour * 60 + now.minute,
        )
        when (RingRules.decide(settings, situation)) {
            RingAction.RING_THROUGH -> RingThrough.start(this)
            RingAction.SILENCE -> {
                CallAnnouncer.stop()
                silenceRinger()
                // The ringtone may not have started yet; make sure once it has.
                delay(SILENCE_RETRY_MILLIS)
                if (CallManager.calls.value.any { it.phase == CallPhase.RINGING }) silenceRinger()
            }
            RingAction.NORMAL -> Unit
        }
    }

    // Allowed for the default phone app, which is the only time this service runs.
    @SuppressLint("MissingPermission")
    private fun silenceRinger() {
        try {
            getSystemService(TelecomManager::class.java).silenceRinger()
        } catch (e: SecurityException) {
            // No longer the phone app: nothing to silence.
        }
    }

    /** Calls from the last few minutes: this app's memory, plus the call log when it's allowed. */
    private fun recentCalls(): List<Pair<String?, Long>> {
        val since = System.currentTimeMillis() - RingRules.REPEAT_WINDOW_MILLIS
        val fromLog = if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            try {
                contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE),
                    "${CallLog.Calls.DATE} > ?",
                    arrayOf(since.toString()),
                    null,
                )?.use { cursor ->
                    buildList<Pair<String?, Long>> {
                        while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getLong(1))
                    }
                }.orEmpty()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        return CallManager.recentIncoming() + fromLog
    }

    private companion object {
        const val SILENCE_RETRY_MILLIS = 1000L
        const val BANNER_DELAY_MILLIS = 1500L
    }

    override fun onDestroy() {
        CallManager.service = null
        CallManager.clear()
        notifier.cancel()
        scope.cancel()
        super.onDestroy()
    }
}
