package io.github.secnewt.dialer.screening

/**
 * Remembers the call the screener just silenced, so the call screen can tell it apart from
 * a call that should ring. Android rings a silenced call without sound but still hands it to
 * the phone app, which would otherwise open the full-screen call window for it.
 *
 * Both services run in this app's process, and Android screens a call before handing it over.
 */
object SilencedCalls {

    /** How long after screening the call may arrive at the call screen and still match. */
    const val MATCH_MILLIS = 15_000L

    private var silencedNumber: String? = null
    private var silencedAt: Long? = null

    @Synchronized
    fun mark(number: String?, nowMillis: Long) {
        silencedNumber = PhoneNumbers.normalize(number)
        silencedAt = nowMillis
    }

    /** True once for the call that was just silenced; the record is then used up. */
    @Synchronized
    fun take(number: String?, nowMillis: Long): Boolean {
        val at = silencedAt ?: return false
        val match = matches(silencedNumber, at, number, nowMillis)
        if (match) {
            silencedNumber = null
            silencedAt = null
        }
        return match
    }

    /** The pure rule: the same number (or both hidden), screened moments ago. */
    fun matches(silencedNumber: String?, silencedAt: Long, number: String?, nowMillis: Long): Boolean {
        val age = nowMillis - silencedAt
        if (age < 0 || age > MATCH_MILLIS) return false
        return silencedNumber == PhoneNumbers.normalize(number)
    }
}
