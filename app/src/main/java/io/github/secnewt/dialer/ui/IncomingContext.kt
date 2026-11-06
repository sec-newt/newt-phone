package io.github.secnewt.dialer.ui

import io.github.secnewt.dialer.calls.LiveCall
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Who's calling, in a line or two for the incoming-call screen: "★ Favorite · mobile" and "Last
 * talked yesterday, 4 min". Empty when nothing is known.
 */
fun incomingContext(call: LiveCall, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): List<String> {
    val who = when {
        call.starred -> listOfNotNull("★ Favorite", call.numberType).joinToString(" · ")
        else -> call.numberType
    }
    val talked = call.lastTalkedMillis?.let { at ->
        val seconds = call.lastTalkedSeconds ?: 0
        val length = if (seconds < 60) "under a minute" else "${seconds / 60}\u00A0min"
        "Last talked ${dayName(at, nowMillis, zone)}, $length"
    }
    return listOfNotNull(who, talked)
}

private fun dayName(atMillis: Long, nowMillis: Long, zone: ZoneId): String {
    val day = Instant.ofEpochMilli(atMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    return when (ChronoUnit.DAYS.between(day, today)) {
        0L -> "today"
        1L -> "yesterday"
        in 2L..6L -> day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        else -> day.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
    }
}
