package io.github.secnewt.dialer.calls

/**
 * Whether an "open" request is for the call history, like the one behind Android's missed-call
 * notification. Kept free of Android types so it can be tested.
 */
object CallHistoryRequest {

    const val ACTION_VIEW = "android.intent.action.VIEW"
    const val CALLS_TYPE = "vnd.android.cursor.dir/calls"
    const val CALLS_URI = "content://call_log/calls"

    fun matches(action: String?, type: String?, data: String?): Boolean =
        action == ACTION_VIEW && (type == CALLS_TYPE || data == CALLS_URI)
}
