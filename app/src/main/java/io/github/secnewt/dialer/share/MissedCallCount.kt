package io.github.secnewt.dialer.share

/**
 * The one thing Newt Phone shares with other Newt apps: how many missed calls you haven't
 * looked at yet. Kept free of Android types so it can be tested.
 */
object MissedCallCount {

    /** The column holding the count, in the single row the provider returns. */
    const val COLUMN = "count"

    /** Only the count path exists; anything else gets nothing. */
    fun isCountPath(path: String?): Boolean = path == "/count"

    /** Never negative, and capped so a broken call log can't show a silly number. */
    fun clamp(raw: Int): Int = raw.coerceIn(0, 999)
}
