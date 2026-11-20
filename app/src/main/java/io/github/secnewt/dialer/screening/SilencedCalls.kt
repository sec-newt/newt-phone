package io.github.secnewt.dialer.screening

/**
 * Remembers the calls the screener just silenced, so the call screen can tell them apart from
 * calls that should ring. Android rings a silenced call without sound but still hands it to
 * the phone app, which would otherwise open the full-screen call window for it.
 *
 * Both services run in this app's process, and Android screens a call before handing it over.
 */
object SilencedCalls {

    /** How long after screening the call may arrive at the call screen and still match. */
    const val MATCH_MILLIS = 15_000L

    /**
     * Android ignores a screening answer given after 5 seconds, so a later SILENCE didn't
     * silence anything and isn't remembered. Kept a little under, to be safe.
     */
    const val SCREENING_DEADLINE_MILLIS = 4_500L

    private class Decision(val number: String?, val atMillis: Long)

    /** One entry per silenced call not yet handed over, oldest first. */
    private val pending = ArrayDeque<Decision>()

    @Synchronized
    fun mark(number: String?, nowMillis: Long) {
        pending.removeAll { nowMillis - it.atMillis > MATCH_MILLIS }
        pending.addLast(Decision(PhoneNumbers.normalize(number), nowMillis))
    }

    /** True once for each call that was just silenced; that decision is then used up. */
    @Synchronized
    fun take(number: String?, nowMillis: Long): Boolean {
        val match = pending.firstOrNull { matches(it.number, it.atMillis, number, nowMillis) } ?: return false
        pending.remove(match)
        return true
    }

    @Synchronized
    internal fun clear() = pending.clear()

    /** Whether a SILENCE answered this long after screening started still counts. */
    fun answeredInTime(elapsedMillis: Long): Boolean = elapsedMillis in 0..SCREENING_DEADLINE_MILLIS

    /** The pure rule: the same number (or both hidden), screened moments ago. */
    fun matches(silencedNumber: String?, silencedAt: Long, number: String?, nowMillis: Long): Boolean {
        val age = nowMillis - silencedAt
        if (age < 0 || age > MATCH_MILLIS) return false
        return silencedNumber == PhoneNumbers.normalize(number)
    }
}
