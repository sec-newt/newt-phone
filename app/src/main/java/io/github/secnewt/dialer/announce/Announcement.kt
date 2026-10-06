package io.github.secnewt.dialer.announce

import io.github.secnewt.dialer.screening.CallAction
import io.github.secnewt.dialer.screening.PhoneNumbers
import io.github.secnewt.dialer.screening.Reason
import io.github.secnewt.dialer.screening.ScreenedCall

/** When the phone should say who is calling. */
enum class AnnounceMode { OFF, ALWAYS, HEADPHONES_ONLY }

data class AnnounceSettings(
    val mode: AnnounceMode = AnnounceMode.OFF,
    /** Stay silent while Do Not Disturb is on. */
    val quietDuringDnd: Boolean = true,
)

/** What the phone is doing right now, gathered just before speaking. */
data class AudioSituation(
    val headphonesConnected: Boolean,
    /** False when the phone is on silent or vibrate. */
    val ringerAudible: Boolean,
    val doNotDisturbOn: Boolean,
)

/** The pure decision and wording for caller announcements, kept separate so it can be tested. */
object Announcement {

    fun shouldSpeak(settings: AnnounceSettings, situation: AudioSituation, screening: ScreenedCall?): Boolean {
        if (settings.mode == AnnounceMode.OFF) return false
        // A call that was really silenced or blocked is not ringing, so say nothing.
        if (screening != null && screening.enforced && screening.action != CallAction.RING) return false
        if (situation.doNotDisturbOn && settings.quietDuringDnd) return false
        // Headphones are private, so speaking there is fine even with the ringer off.
        if (situation.headphonesConnected) return true
        return settings.mode == AnnounceMode.ALWAYS && situation.ringerAudible
    }

    /**
     * Calling apps like Teams also make the phone report "ringing", but never share the
     * caller's number. Without a real phone call ringing, a missing number means an app call,
     * not a private number, so it is left to that app to announce.
     */
    fun isAppCall(number: String?, phoneCallRinging: Boolean): Boolean =
        PhoneNumbers.normalize(number) == null && !phoneCallRinging

    /** "Call from Mom", "Call from 5 5 5, 0 1 9, 7 7 3 1", "Likely spam, from …". */
    fun text(contactName: String?, number: String?, screening: ScreenedCall?): String {
        if (!contactName.isNullOrBlank()) return "Call from $contactName"
        val digits = PhoneNumbers.normalize(number) ?: return "Call from a private number"
        val spoken = spellOut(digits)
        val flagged = screening != null && screening.action != CallAction.RING
        return when {
            flagged && screening?.reason == Reason.BLOCK_LIST -> "Blocked number, from $spoken"
            flagged -> "Likely spam, from $spoken"
            else -> "Call from $spoken"
        }
    }

    /** Reads digits one at a time, grouped like a US number: "5 5 5, 0 1 9, 7 7 3 1". */
    fun spellOut(digits: String): String {
        val groups = if (digits.length == 10) {
            listOf(digits.substring(0, 3), digits.substring(3, 6), digits.substring(6))
        } else {
            digits.chunked(4)
        }
        return groups.joinToString(", ") { it.toList().joinToString(" ") }
    }
}
