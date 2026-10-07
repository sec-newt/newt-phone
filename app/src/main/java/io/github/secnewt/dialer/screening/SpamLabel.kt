package io.github.secnewt.dialer.screening

import kotlin.math.abs

/** The warning shown next to a call that the screener flagged. */
object SpamLabel {

    /** How far apart a call and its screening entry can be and still be the same call. */
    private const val MATCH_WINDOW_MILLIS = 2 * 60_000L

    /**
     * "Blocked number" or "Likely spam" when the screening log flagged this call, matched by
     * number and time; null when it wasn't flagged or the number is hidden.
     */
    fun forCall(number: String?, timeMillis: Long, screened: List<ScreenedCall>): String? {
        val wanted = PhoneNumbers.normalize(number) ?: return null
        val match = screened.firstOrNull {
            PhoneNumbers.normalize(it.number) == wanted && abs(it.timeMillis - timeMillis) < MATCH_WINDOW_MILLIS
        } ?: return null
        return when {
            match.reason == Reason.BLOCK_LIST -> "Blocked number"
            match.action != CallAction.RING || match.verification == Verification.FAILED -> "Likely spam"
            else -> null
        }
    }
}
