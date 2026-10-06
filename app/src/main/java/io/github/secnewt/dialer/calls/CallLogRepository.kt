package io.github.secnewt.dialer.calls

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog.Calls

/** Reads the phone's own call history (the same list the stock phone app shows). */
class CallLogRepository(private val context: Context) {

    fun hasAccess(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    /** Newest first, at most [limit] calls. */
    fun load(limit: Int = 300): List<LoggedCall> {
        val calls = mutableListOf<LoggedCall>()
        context.contentResolver.query(
            Calls.CONTENT_URI,
            arrayOf(
                Calls._ID, Calls.NUMBER, Calls.CACHED_NAME, Calls.TYPE, Calls.DATE,
                Calls.DURATION, Calls.CACHED_PHOTO_URI, Calls.NUMBER_PRESENTATION,
            ),
            null, null,
            "${Calls.DATE} DESC",
        )?.use { cursor ->
            while (cursor.moveToNext() && calls.size < limit) {
                val hidden = cursor.getInt(7) != Calls.PRESENTATION_ALLOWED
                calls += LoggedCall(
                    id = cursor.getLong(0),
                    number = if (hidden) null else cursor.getString(1),
                    name = cursor.getString(2)?.takeIf { it.isNotBlank() },
                    kind = kindOf(cursor.getInt(3)),
                    timeMillis = cursor.getLong(4),
                    durationSeconds = cursor.getLong(5),
                    photoUri = cursor.getString(6),
                )
            }
        }
        return calls
    }

    private fun kindOf(type: Int): CallKind = when (type) {
        Calls.INCOMING_TYPE, Calls.ANSWERED_EXTERNALLY_TYPE -> CallKind.INCOMING
        Calls.OUTGOING_TYPE -> CallKind.OUTGOING
        Calls.MISSED_TYPE -> CallKind.MISSED
        Calls.REJECTED_TYPE -> CallKind.DECLINED
        Calls.BLOCKED_TYPE -> CallKind.BLOCKED
        Calls.VOICEMAIL_TYPE -> CallKind.VOICEMAIL
        else -> CallKind.OTHER
    }
}
