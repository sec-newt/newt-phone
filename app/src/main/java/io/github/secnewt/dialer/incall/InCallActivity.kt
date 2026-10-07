package io.github.secnewt.dialer.incall

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import io.github.secnewt.dialer.MainActivity
import io.github.secnewt.dialer.calls.CallPhase
import io.github.secnewt.dialer.calls.LiveCall
import io.github.secnewt.dialer.calls.LiveCalls
import io.github.secnewt.dialer.ui.CallActions
import io.github.secnewt.dialer.ui.CallScreen
import io.github.secnewt.dialer.ui.ContactsPhotoLoader
import io.github.secnewt.dialer.ui.LocalPhotoLoader
import io.github.secnewt.dialer.ui.theme.DialerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** The incoming-call and in-call screen. Shows over the lock screen and wakes the display. */
class InCallActivity : ComponentActivity() {

    private var proximityLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        // Back leaves the call running and goes to whatever was open before.
        onBackPressedDispatcher.addCallback(this) { moveTaskToBack(true) }

        lifecycleScope.launch {
            combine(CallManager.calls, CallManager.audio) { calls, audio -> LiveCalls.wantsProximitySensor(calls, audio) }
                .distinctUntilChanged()
                .collect { setProximitySensor(it) }
        }

        val actions = CallActions(
            onAnswer = CallManager::answer,
            onDecline = CallManager::decline,
            onHangUp = CallManager::hangUp,
            onMute = CallManager::setMuted,
            onRoute = CallManager::setRoute,
            onToggleHold = CallManager::toggleHold,
            onSwap = CallManager::swap,
            onMerge = CallManager::merge,
            onTone = CallManager::playTone,
            onAddCall = ::addCall,
        )
        val photoLoader = ContactsPhotoLoader(this)
        setContent {
            DialerTheme {
                CompositionLocalProvider(LocalPhotoLoader provides photoLoader) {
                    val calls by CallManager.calls.collectAsState()
                    val audio by CallManager.audio.collectAsState()
                    var last by remember { mutableStateOf<LiveCall?>(null) }
                    val main = LiveCalls.primary(calls)
                    LaunchedEffect(main) { if (main != null) last = main }
                    LaunchedEffect(calls.isEmpty()) {
                        if (calls.isEmpty()) {
                            // Show "Call ended" for a moment, then get out of the way.
                            if (last != null) delay(ENDED_SCREEN_MILLIS)
                            finishAndRemoveTask()
                        }
                    }
                    val shown = main ?: last?.copy(phase = CallPhase.ENDED)
                    if (shown != null) {
                        CallScreen(call = shown, other = LiveCalls.secondary(calls), audio = audio, actions = actions)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        setProximitySensor(false)
        super.onDestroy()
    }

    /** Turns the screen off against your ear so your cheek can't press buttons. */
    private fun setProximitySensor(on: Boolean) {
        if (on) {
            if (proximityLock != null) return
            val power = getSystemService(PowerManager::class.java)
            if (!power.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) return
            proximityLock = power.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "dialer:proximity").also {
                it.acquire(PROXIMITY_TIMEOUT_MILLIS)
            }
        } else {
            proximityLock?.let { if (it.isHeld) it.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY) }
            proximityLock = null
        }
    }

    /** Opens the dialpad for a second call; the first goes on hold when it connects. */
    private fun addCall() {
        getSystemService(KeyguardManager::class.java).requestDismissKeyguard(this, null)
        startActivity(Intent(this, MainActivity::class.java).setAction(Intent.ACTION_DIAL))
    }

    companion object {
        private const val ENDED_SCREEN_MILLIS = 1500L
        private const val PROXIMITY_TIMEOUT_MILLIS = 4 * 60 * 60 * 1000L

        fun intent(context: Context): Intent = Intent(context, InCallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
