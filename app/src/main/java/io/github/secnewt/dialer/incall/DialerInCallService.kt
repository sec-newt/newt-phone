package io.github.secnewt.dialer.incall

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
        scope.launch { CallManager.calls.collect { notifier.update(it) } }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val id = CallManager.add(call)
        val number = CallManager.numberOf(call)
        scope.launch {
            val info = withContext(Dispatchers.IO) { CallerLookup(this@DialerInCallService).lookup(number) }
            CallManager.setCallerInfo(id, info)
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

    override fun onDestroy() {
        CallManager.service = null
        CallManager.clear()
        notifier.cancel()
        scope.cancel()
        super.onDestroy()
    }
}
