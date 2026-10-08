package io.github.secnewt.dialer.incall

import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import io.github.secnewt.dialer.announce.CallAnnouncer
import io.github.secnewt.dialer.calls.AudioRoute
import io.github.secnewt.dialer.calls.AudioState
import io.github.secnewt.dialer.calls.CallPhase
import io.github.secnewt.dialer.calls.LiveCall
import io.github.secnewt.dialer.calls.LiveCalls
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What we found out about a caller after the call arrived. */
data class CallerInfo(
    val name: String? = null,
    val photoUri: String? = null,
    val spamLabel: String? = null,
    val isEmergency: Boolean = false,
)

/**
 * The calls Android has handed to this app, shared by the call screen and the notification.
 * Everything here runs on the main thread, where Telecom delivers its callbacks.
 */
object CallManager {

    private val _calls = MutableStateFlow<List<LiveCall>>(emptyList())
    val calls: StateFlow<List<LiveCall>> = _calls.asStateFlow()

    private val _audio = MutableStateFlow(AudioState())
    val audio: StateFlow<AudioState> = _audio.asStateFlow()

    private val telecomCalls = LinkedHashMap<String, Call>()
    private val callerInfo = HashMap<String, CallerInfo>()
    private var nextId = 0

    /** True while the call screen is on screen, so the notification doesn't pop up over it. */
    val screenShowing = MutableStateFlow(false)

    /** Set while Android has the in-call service bound; it controls mute and the speaker. */
    internal var service: InCallService? = null

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = publish()
        override fun onDetailsChanged(call: Call, details: Call.Details) = publish()
        override fun onParentChanged(call: Call, parent: Call?) = publish()
        override fun onChildrenChanged(call: Call, children: List<Call>) = publish()
        override fun onConferenceableCallsChanged(call: Call, conferenceableCalls: List<Call>) = publish()
    }

    fun add(call: Call): String {
        val id = "call-${nextId++}"
        telecomCalls[id] = call
        call.registerCallback(callback)
        publish()
        return id
    }

    fun remove(call: Call) {
        val id = idOf(call) ?: return
        call.unregisterCallback(callback)
        telecomCalls.remove(id)
        callerInfo.remove(id)
        publish()
    }

    fun clear() {
        telecomCalls.values.forEach { it.unregisterCallback(callback) }
        telecomCalls.clear()
        callerInfo.clear()
        publish()
    }

    fun setCallerInfo(id: String, info: CallerInfo) {
        if (id !in telecomCalls) return
        callerInfo[id] = info
        publish()
    }

    /** The number a call is from or to, or null when it's hidden or is voicemail. */
    fun numberOf(call: Call): String? {
        val details = call.details
        val handle = details.handle ?: return null
        if (details.handlePresentation != TelecomManager.PRESENTATION_ALLOWED) return null
        if (handle.scheme != PhoneAccount.SCHEME_TEL) return null
        return handle.schemeSpecificPart?.takeIf { it.isNotBlank() }
    }

    fun answer(id: String) {
        CallAnnouncer.stop()
        telecomCalls[id]?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun decline(id: String) {
        CallAnnouncer.stop()
        telecomCalls[id]?.reject(false, null)
    }

    fun hangUp(id: String) {
        telecomCalls[id]?.disconnect()
    }

    fun toggleHold(id: String) {
        val call = telecomCalls[id] ?: return
        when (phaseOf(call)) {
            CallPhase.ON_HOLD -> call.unhold()
            CallPhase.ACTIVE -> if (call.details.can(Call.Details.CAPABILITY_HOLD)) call.hold()
            else -> Unit
        }
    }

    /** Picks up the call on hold; Android puts the current one on hold for you. */
    fun swap() {
        telecomCalls.values.firstOrNull { phaseOf(it) == CallPhase.ON_HOLD }?.unhold()
    }

    /** Joins this call and the one it can be merged with into a conference. */
    fun merge(id: String) {
        val call = telecomCalls[id] ?: return
        val other = call.conferenceableCalls.firstOrNull()
        when {
            other != null -> call.conference(other)
            call.details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE) -> call.mergeConference()
        }
    }

    fun playTone(id: String, digit: Char) {
        val call = telecomCalls[id] ?: return
        call.playDtmfTone(digit)
        call.stopDtmfTone()
    }

    fun setMuted(muted: Boolean) {
        service?.setMuted(muted)
    }

    @Suppress("DEPRECATION") // CallEndpoint replaces this on Android 14, but the app supports 11.
    fun setRoute(route: AudioRoute) {
        val value = when (route) {
            AudioRoute.EARPIECE -> CallAudioState.ROUTE_EARPIECE
            AudioRoute.WIRED -> CallAudioState.ROUTE_WIRED_HEADSET
            AudioRoute.SPEAKER -> CallAudioState.ROUTE_SPEAKER
            AudioRoute.BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
        }
        service?.setAudioRoute(value)
    }

    @Suppress("DEPRECATION")
    fun updateAudio(state: CallAudioState) {
        val routes = mapOf(
            CallAudioState.ROUTE_EARPIECE to AudioRoute.EARPIECE,
            CallAudioState.ROUTE_WIRED_HEADSET to AudioRoute.WIRED,
            CallAudioState.ROUTE_SPEAKER to AudioRoute.SPEAKER,
            CallAudioState.ROUTE_BLUETOOTH to AudioRoute.BLUETOOTH,
        )
        _audio.value = AudioState(
            route = routes[state.route] ?: AudioRoute.EARPIECE,
            available = routes.filterKeys { state.supportedRouteMask and it != 0 }.values.toSet(),
            muted = state.isMuted,
        )
    }

    private fun idOf(call: Call): String? = telecomCalls.entries.firstOrNull { it.value == call }?.key

    private fun phaseOf(call: Call): CallPhase {
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details.state
        } else {
            @Suppress("DEPRECATION")
            call.state
        }
        return LiveCalls.phaseOf(state)
    }

    private fun publish() {
        _calls.value = LiveCalls.withParticipants(telecomCalls.map { (id, call) -> toLiveCall(id, call) })
    }

    private fun toLiveCall(id: String, call: Call): LiveCall {
        val details = call.details
        val info = callerInfo[id] ?: CallerInfo()
        val shownName = details.callerDisplayName?.takeIf {
            it.isNotBlank() && details.callerDisplayNamePresentation == TelecomManager.PRESENTATION_ALLOWED
        }
        return LiveCall(
            id = id,
            number = numberOf(call),
            name = info.name ?: details.contactDisplayName?.takeIf { it.isNotBlank() } ?: shownName,
            phase = phaseOf(call),
            connectedAtMillis = details.connectTimeMillis.takeIf { it > 0 },
            photoUri = info.photoUri,
            spamLabel = info.spamLabel,
            canHold = details.can(Call.Details.CAPABILITY_HOLD),
            isEmergency = info.isEmergency,
            isVoicemail = details.handle?.scheme == PhoneAccount.SCHEME_VOICEMAIL,
            isConference = details.hasProperty(Call.Details.PROPERTY_CONFERENCE),
            parentId = call.parent?.let(::idOf),
            canMerge = call.conferenceableCalls.isNotEmpty() ||
                details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE),
        )
    }
}
