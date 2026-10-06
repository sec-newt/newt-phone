package io.github.secnewt.dialer.calls

import io.github.secnewt.dialer.screening.PhoneNumbers
import java.time.Instant
import java.time.ZoneId

/** What happened on a call, in the words shown on screen. */
enum class CallKind(val label: String) {
    INCOMING("Incoming"),
    OUTGOING("Outgoing"),
    MISSED("Missed"),
    DECLINED("Declined"),
    BLOCKED("Blocked"),
    VOICEMAIL("Voicemail"),
    OTHER("Call"),
}

/** One entry from the phone's call history. */
data class LoggedCall(
    val id: Long,
    /** Null or blank for hidden numbers. */
    val number: String?,
    /** The contact name at the time of the call, if it was a contact. */
    val name: String?,
    val kind: CallKind,
    val timeMillis: Long,
    val durationSeconds: Long = 0,
    val photoUri: String? = null,
)

/** Back-to-back calls with the same person on the same day, shown as one row with a count. */
data class CallGroup(val calls: List<LoggedCall>) {
    val latest: LoggedCall get() = calls.first()
    val count: Int get() = calls.size
}

/** Grouping and wording for the Recents tab, kept free of Android so it can be tested. */
object CallHistory {

    /** Groups consecutive calls (newest first) from the same number on the same day. */
    fun group(calls: List<LoggedCall>, zone: ZoneId): List<CallGroup> {
        val groups = mutableListOf<MutableList<LoggedCall>>()
        for (call in calls) {
            val current = groups.lastOrNull()
            if (current != null && sameCaller(current.first(), call) && sameDay(current.first(), call, zone)) {
                current += call
            } else {
                groups += mutableListOf(call)
            }
        }
        return groups.map { CallGroup(it) }
    }

    private fun sameCaller(a: LoggedCall, b: LoggedCall): Boolean =
        PhoneNumbers.normalize(a.number) == PhoneNumbers.normalize(b.number)

    private fun sameDay(a: LoggedCall, b: LoggedCall, zone: ZoneId): Boolean =
        Instant.ofEpochMilli(a.timeMillis).atZone(zone).toLocalDate() ==
            Instant.ofEpochMilli(b.timeMillis).atZone(zone).toLocalDate()

    /** "Missed", "Outgoing, 3 calls", "Incoming, 4 minutes". */
    fun summary(group: CallGroup): String {
        val parts = mutableListOf(group.latest.kind.label)
        if (group.count > 1) {
            parts += "${group.count} calls"
        } else if (group.latest.kind in setOf(CallKind.INCOMING, CallKind.OUTGOING)) {
            duration(group.latest.durationSeconds)?.let { parts += it }
        }
        return parts.joinToString(", ")
    }

    /** "45 seconds", "1 minute", "12 minutes", "1 hour 5 minutes"; null when the call didn't connect. */
    fun duration(seconds: Long): String? {
        if (seconds <= 0) return null
        if (seconds < 60) return "$seconds seconds"
        val minutes = seconds / 60
        if (minutes < 60) return if (minutes == 1L) "1 minute" else "$minutes minutes"
        val hours = minutes / 60
        val rest = minutes % 60
        val hourText = if (hours == 1L) "1 hour" else "$hours hours"
        return when (rest) {
            0L -> hourText
            1L -> "$hourText 1 minute"
            else -> "$hourText $rest minutes"
        }
    }
}
