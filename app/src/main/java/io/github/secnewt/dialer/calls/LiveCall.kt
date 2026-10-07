package io.github.secnewt.dialer.calls

/** Where a live call is, in the terms the call screen uses. */
enum class CallPhase { RINGING, DIALING, ACTIVE, ON_HOLD, ENDED }

/** Where the call's sound goes. */
enum class AudioRoute(val label: String) {
    EARPIECE("Phone"),
    WIRED("Headset"),
    SPEAKER("Speaker"),
    BLUETOOTH("Bluetooth"),
}

/** A call happening right now, as the call screen shows it. */
data class LiveCall(
    val id: String,
    /** The caller's number, or null when it is hidden. */
    val number: String?,
    /** The contact's name, when the number is saved. */
    val name: String?,
    val phase: CallPhase,
    /** When the call was answered (wall clock), or null if it hasn't been. */
    val connectedAtMillis: Long? = null,
    val photoUri: String? = null,
    /** "Likely spam" or "Blocked number" from the screener, or null. */
    val spamLabel: String? = null,
    val canHold: Boolean = false,
    val isEmergency: Boolean = false,
    val isVoicemail: Boolean = false,
)

/** Sound settings for the calls in progress. */
data class AudioState(
    val route: AudioRoute = AudioRoute.EARPIECE,
    val available: Set<AudioRoute> = setOf(AudioRoute.EARPIECE, AudioRoute.SPEAKER),
    val muted: Boolean = false,
)

/** Rules for the call screen, kept free of Android so they can be tested. */
object LiveCalls {

    // android.telecom.Call.STATE_* values; copied so these rules run without Android.
    private const val STATE_NEW = 0
    private const val STATE_DIALING = 1
    private const val STATE_RINGING = 2
    private const val STATE_HOLDING = 3
    private const val STATE_ACTIVE = 4
    private const val STATE_DISCONNECTED = 7
    private const val STATE_SELECT_PHONE_ACCOUNT = 8
    private const val STATE_CONNECTING = 9
    private const val STATE_DISCONNECTING = 10
    private const val STATE_PULLING_CALL = 11
    private const val STATE_AUDIO_PROCESSING = 12
    private const val STATE_SIMULATED_RINGING = 13

    fun phaseOf(state: Int): CallPhase = when (state) {
        STATE_RINGING, STATE_SIMULATED_RINGING -> CallPhase.RINGING
        STATE_ACTIVE, STATE_AUDIO_PROCESSING -> CallPhase.ACTIVE
        STATE_HOLDING -> CallPhase.ON_HOLD
        STATE_DISCONNECTED, STATE_DISCONNECTING -> CallPhase.ENDED
        STATE_NEW, STATE_DIALING, STATE_CONNECTING, STATE_SELECT_PHONE_ACCOUNT, STATE_PULLING_CALL -> CallPhase.DIALING
        else -> CallPhase.DIALING
    }

    /**
     * The call the screen is about: a ringing call first (it needs an answer), then the one
     * you're talking on, then one being dialed, then one on hold.
     */
    fun primary(calls: List<LiveCall>): LiveCall? {
        val order = listOf(CallPhase.RINGING, CallPhase.ACTIVE, CallPhase.DIALING, CallPhase.ON_HOLD, CallPhase.ENDED)
        return order.firstNotNullOfOrNull { phase -> calls.firstOrNull { it.phase == phase } }
    }

    /** The other call still going (on hold, or the one a ringing call would interrupt). */
    fun secondary(calls: List<LiveCall>): LiveCall? {
        val main = primary(calls) ?: return null
        return calls.firstOrNull { it.id != main.id && it.phase != CallPhase.ENDED }
    }

    /** "0:07", "12:34" or "1:02:03". */
    fun elapsed(millis: Long): String {
        val total = (millis / 1000).coerceAtLeast(0)
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    /** The line under the caller's name: "Incoming call", "Calling…", the timer, "On hold". */
    fun status(call: LiveCall, nowMillis: Long): String = when (call.phase) {
        CallPhase.RINGING -> "Incoming call"
        CallPhase.DIALING -> if (call.isEmergency) "Emergency call" else "Calling…"
        CallPhase.ACTIVE -> call.connectedAtMillis?.let { elapsed(nowMillis - it) } ?: "Connected"
        CallPhase.ON_HOLD -> "On hold"
        CallPhase.ENDED -> "Call ended"
    }

    /**
     * The audio button's next stop. Without Bluetooth it simply turns the speaker on and off;
     * with Bluetooth it goes phone (or headset), speaker, Bluetooth, and around again.
     */
    fun nextRoute(audio: AudioState): AudioRoute {
        val handset = if (AudioRoute.WIRED in audio.available) AudioRoute.WIRED else AudioRoute.EARPIECE
        val order = listOfNotNull(
            handset,
            AudioRoute.SPEAKER,
            AudioRoute.BLUETOOTH.takeIf { it in audio.available },
        )
        val index = order.indexOf(audio.route)
        return order[(index + 1) % order.size]
    }

    /** Bluetooth turns the audio button into a three-way switch instead of a speaker toggle. */
    fun audioIsSwitch(audio: AudioState): Boolean = AudioRoute.BLUETOOTH in audio.available

    /** The screen goes dark against your ear only while the sound comes out of the earpiece. */
    fun wantsProximitySensor(calls: List<LiveCall>, audio: AudioState): Boolean {
        val main = primary(calls) ?: return false
        return audio.route == AudioRoute.EARPIECE && main.phase in setOf(CallPhase.DIALING, CallPhase.ACTIVE)
    }
}
