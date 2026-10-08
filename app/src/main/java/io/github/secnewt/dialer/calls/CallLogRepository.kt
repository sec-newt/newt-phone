package io.github.secnewt.dialer.calls

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog.Calls
import android.provider.ContactsContract.PhoneLookup

/** Reads the phone's own call history (the same list the stock phone app shows). */
class CallLogRepository(private val context: Context) {

    fun hasAccess(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    /**
     * Newest first, at most [limit] calls. Names and photos come from the contacts as they are
     * now (Android's call history keeps the name from the time of the call), when allowed.
     */
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
        return withCurrentNames(calls)
    }

    private fun withCurrentNames(calls: List<LoggedCall>): List<LoggedCall> {
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return calls
        }
        val current = calls.mapNotNull { it.number }.distinct().associateWith(::lookUp)
        return calls.map { call ->
            // Couldn't check: keep what the call history says.
            val (name, photo) = call.number?.let { current[it] } ?: return@map call
            call.copy(name = name, photoUri = photo)
        }
    }

    /** The saved contact's name and photo for [number] (nulls when it isn't saved), or null if unknown. */
    private fun lookUp(number: String): Pair<String?, String?>? = try {
        val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        context.contentResolver.query(uri, arrayOf(PhoneLookup.DISPLAY_NAME, PhoneLookup.PHOTO_THUMBNAIL_URI), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) to it.getString(1) else null to null }
            ?: (null to null)
    } catch (e: Exception) {
        null
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
