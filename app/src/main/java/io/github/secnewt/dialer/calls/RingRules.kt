package io.github.secnewt.dialer.calls

import io.github.secnewt.dialer.screening.PhoneNumbers

/** A daily time window, in minutes after midnight. It may wrap past midnight (22:00 to 07:00). */
data class QuietHours(
    val enabled: Boolean = false,
    val startMinutes: Int = 22 * 60,
    val endMinutes: Int = 7 * 60,
)

/** Who gets through when the phone is on silent, and when quiet hours are. */
data class RingSettings(
    /** Starred contacts ring even when the phone is on silent or vibrate. */
    val starredRingThrough: Boolean = true,
    /** A number that calls again within [RingRules.REPEAT_WINDOW_MILLIS] rings even on silent. */
    val repeatRingThrough: Boolean = true,
    val quietHours: QuietHours = QuietHours(),
)

/** What to do with a ringing call, on top of what Android already does. */
enum class RingAction {
    /** Leave it to Android. */
    NORMAL,

    /** The phone is on silent but this caller should be heard: play the ringtone ourselves. */
    RING_THROUGH,

    /** Quiet hours: show the call but stop the ringtone. */
    SILENCE,
}

/** What is known about a ringing call and the phone at that moment. */
data class RingSituation(
    /** False when the phone is on silent or vibrate. */
    val ringerAudible: Boolean,
    val starred: Boolean,
    val repeatCaller: Boolean,
    /** The screener flagged it (likely spam or blocked); never let those through. */
    val flagged: Boolean,
    val minutesAfterMidnight: Int,
)

/** The rules, kept free of Android so they can be tested. */
object RingRules {

    /** "Calls again within 3 minutes" counts as a repeat caller. */
    const val REPEAT_WINDOW_MILLIS = 3 * 60_000L

    fun decide(settings: RingSettings, call: RingSituation): RingAction {
        if (call.flagged) return RingAction.NORMAL
        val special = (call.starred && settings.starredRingThrough) || (call.repeatCaller && settings.repeatRingThrough)
        return when {
            special && !call.ringerAudible -> RingAction.RING_THROUGH
            !special && call.ringerAudible && inQuietHours(settings.quietHours, call.minutesAfterMidnight) ->
                RingAction.SILENCE
            else -> RingAction.NORMAL
        }
    }

    fun inQuietHours(hours: QuietHours, minutesAfterMidnight: Int): Boolean {
        if (!hours.enabled || hours.startMinutes == hours.endMinutes) return false
        val now = minutesAfterMidnight
        return if (hours.startMinutes < hours.endMinutes) {
            now >= hours.startMinutes && now < hours.endMinutes
        } else {
            now >= hours.startMinutes || now < hours.endMinutes
        }
    }

    /** True when [number] called within the repeat window before [nowMillis]. */
    fun isRepeat(number: String?, nowMillis: Long, earlierCalls: List<Pair<String?, Long>>): Boolean {
        val key = PhoneNumbers.normalize(number) ?: return false
        return earlierCalls.any { (other, time) ->
            nowMillis - time in 0..REPEAT_WINDOW_MILLIS &&
                PhoneNumbers.normalize(other) == key
        }
    }
}
