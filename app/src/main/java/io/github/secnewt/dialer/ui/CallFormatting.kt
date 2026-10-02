package io.github.secnewt.dialer.ui

import android.telephony.PhoneNumberUtils
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** "(555) 019-7731" for US numbers, the raw number otherwise, "Hidden number" when there is none. */
fun formatCaller(number: String?, countryIso: String = Locale.getDefault().country.ifEmpty { "US" }): String {
    if (number.isNullOrBlank()) return "Hidden number"
    return PhoneNumberUtils.formatNumber(number, countryIso) ?: number
}

/** "Today, 3:42 PM", "Yesterday, 9:05 AM" or "Mon, Sep 29, 3:42 PM". */
fun formatCallTime(timeMillis: Long, now: ZonedDateTime): String {
    val time = Instant.ofEpochMilli(timeMillis).atZone(now.zone)
    val clock = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    val days = now.toLocalDate().toEpochDay() - time.toLocalDate().toEpochDay()
    val day = when (days) {
        0L -> "Today"
        1L -> "Yesterday"
        else -> time.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    }
    return "$day, $clock"
}
