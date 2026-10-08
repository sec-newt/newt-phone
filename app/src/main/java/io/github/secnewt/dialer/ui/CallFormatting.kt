package io.github.secnewt.dialer.ui

import android.telephony.PhoneNumberUtils
import io.github.secnewt.dialer.screening.BlockRule
import io.github.secnewt.dialer.screening.CallAction
import io.github.secnewt.dialer.screening.PhoneNumbers
import io.github.secnewt.dialer.screening.Reason
import io.github.secnewt.dialer.screening.ScreenedCall
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * "(214) 555-0199" for US numbers, including ones saved as "+1 214…", so they fit on one line;
 * other numbers in their usual format; "Hidden number" when there is none.
 */
fun formatCaller(number: String?, countryIso: String = Locale.getDefault().country.ifEmpty { "US" }): String {
    if (number.isNullOrBlank()) return "Hidden number"
    val trimmed = number.trim()
    val digits = PhoneNumbers.normalize(trimmed)
    val usNumber = countryIso.equals("US", ignoreCase = true) &&
        digits?.length == 10 &&
        (!trimmed.startsWith("+") || trimmed.startsWith("+1"))
    if (usNumber && digits != null) {
        return "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
    }
    return PhoneNumberUtils.formatNumber(trimmed, countryIso) ?: trimmed
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

/** "Ring", "Silence" or "Block". */
fun actionLabel(action: CallAction): String = when (action) {
    CallAction.RING -> "Ring"
    CallAction.SILENCE -> "Silence"
    CallAction.BLOCK -> "Block"
}

/**
 * What happened to a screened call, e.g. "Blocked: on your block list" or, in
 * observe-only mode, "Would silence: hidden number". Null when it simply rang.
 */
fun outcomeLabel(call: ScreenedCall): String? {
    if (call.action == CallAction.RING) return null
    val verb = when (call.action) {
        CallAction.SILENCE -> if (call.enforced) "Silenced" else "Would silence"
        CallAction.BLOCK -> if (call.enforced) "Blocked" else "Would block"
        CallAction.RING -> return null
    }
    val why = when (call.reason) {
        Reason.BLOCK_LIST -> "on your block list"
        Reason.HIDDEN_NUMBER -> "hidden number"
        Reason.COPYCAT -> "copies your number"
        Reason.FAILED_VERIFICATION -> "failed carrier verification"
        Reason.NONE -> return verb
    }
    return "$verb: $why"
}

/** "(555) 019-7731" or "Numbers starting with 800 555". */
fun ruleLabel(rule: BlockRule): String = when (rule) {
    is BlockRule.Number -> formatCaller(rule.number)
    is BlockRule.StartsWith -> "Numbers starting with ${rule.digits}"
}
