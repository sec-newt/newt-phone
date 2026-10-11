package io.github.secnewt.dialer.calls

/**
 * "Find this person": a dial request with a name instead of a number, sent by Newt Home's
 * `call mom` command. It only fills in the Contacts search; nothing is ever called by it.
 */
object ContactSearchRequest {

    const val ACTION_DIAL = "android.intent.action.DIAL"
    const val EXTRA_CONTACT_QUERY = "io.github.secnewt.extra.CONTACT_QUERY"

    /** The name to search for, or null when this isn't a search request. */
    fun query(action: String?, hasNumber: Boolean, query: String?): String? =
        query?.trim()?.take(MAX_LENGTH)?.takeIf { action == ACTION_DIAL && !hasNumber && it.isNotEmpty() }

    private const val MAX_LENGTH = 100
}
