package io.github.secnewt.dialer.calls

import io.github.secnewt.dialer.screening.PhoneNumbers
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

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
    /** The contact's current name, if the number is saved (or the name at the time of the call). */
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

    /** "Missed", "Outgoing · 3 calls", "Incoming · 4 min". Short, so a row fits on one line. */
    fun summary(group: CallGroup): String {
        val parts = mutableListOf(group.latest.kind.label)
        if (group.count > 1) {
            parts += "${group.count} calls"
        } else if (group.latest.kind in setOf(CallKind.INCOMING, CallKind.OUTGOING)) {
            duration(group.latest.durationSeconds)?.let { parts += it }
        }
        return parts.joinToString(" · ")
    }

    /** "45 sec", "1 min", "12 min", "1 hr 5 min"; null when the call didn't connect. */
    fun duration(seconds: Long): String? {
        if (seconds <= 0) return null
        if (seconds < 60) return "$seconds sec"
        val minutes = seconds / 60
        if (minutes < 60) return "$minutes min"
        val hours = minutes / 60
        val rest = minutes % 60
        return if (rest == 0L) "$hours hr" else "$hours hr $rest min"
    }

    /** Heading for a day of calls: "Today", "Yesterday", "Thursday", "Thu, Sep 24", "Thu, Sep 24, 2025". */
    fun dayLabel(timeMillis: Long, now: ZonedDateTime): String {
        val date = Instant.ofEpochMilli(timeMillis).atZone(now.zone).toLocalDate()
        val days = now.toLocalDate().toEpochDay() - date.toEpochDay()
        return when {
            days == 0L -> "Today"
            days == 1L -> "Yesterday"
            days in 2..6 -> date.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
            date.year == now.year -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
            else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault()))
        }
    }

    /** "4:03 PM" in the phone's own time format. */
    fun clock(timeMillis: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(timeMillis).atZone(zone).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
}
