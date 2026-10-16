package io.github.secnewt.dialer.screening

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog.Calls
import android.provider.ContactsContract.PhoneLookup
import io.github.secnewt.dialer.calls.RingRules

/**
 * Reads what the phone already knows about a caller: whether they're a contact, whether I've
 * called them, and whether they just called. Nothing leaves the phone.
 */
class CallerHistoryReader(private val context: Context) {

    fun read(number: String, nowMillis: Long, screened: List<ScreenedCall>): CallerHistory {
        val earlier = screened.map { it.number to it.timeMillis }
        val repeat = RingRules.isRepeat(number, nowMillis, earlier)
        if (!allowed(Manifest.permission.READ_CONTACTS) || !allowed(Manifest.permission.READ_CALL_LOG)) {
            return CallerHistory(repeatCaller = repeat)
        }
        return try {
            CallerHistory(
                inContacts = inContacts(number),
                calledBefore = calledBefore(number),
                repeatCaller = repeat || calledRecently(number, nowMillis),
                known = true,
            )
        } catch (e: Exception) {
            // Couldn't read the history: treat nobody as new, so the call rings.
            CallerHistory(repeatCaller = repeat)
        }
    }

    private fun allowed(permission: String) =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun inContacts(number: String): Boolean {
        val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return context.contentResolver.query(uri, arrayOf(PhoneLookup._ID), null, null, null)
            ?.use { it.moveToFirst() } ?: false
    }

    /** Any outgoing call to this number in the call log. */
    private fun calledBefore(number: String): Boolean {
        val wanted = PhoneNumbers.normalize(number) ?: return false
        // The log stores numbers however they were dialed, so narrow by the last digits, then compare.
        val tail = wanted.takeLast(7)
        return context.contentResolver.query(
            Calls.CONTENT_URI,
            arrayOf(Calls.NUMBER),
            "${Calls.TYPE} = ? AND ${Calls.NUMBER} LIKE ?",
            arrayOf(Calls.OUTGOING_TYPE.toString(), "%$tail"),
            null,
        )?.use { cursor ->
            var found = false
            while (!found && cursor.moveToNext()) found = PhoneNumbers.normalize(cursor.getString(0)) == wanted
            found
        } ?: false
    }

    /** A call from this number in the last few minutes, in the call log. */
    private fun calledRecently(number: String, nowMillis: Long): Boolean {
        val since = nowMillis - RingRules.REPEAT_WINDOW_MILLIS
        val earlier = context.contentResolver.query(
            Calls.CONTENT_URI,
            arrayOf(Calls.NUMBER, Calls.DATE),
            "${Calls.DATE} > ?",
            arrayOf(since.toString()),
            null,
        )?.use { cursor ->
            buildList<Pair<String?, Long>> {
                while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getLong(1))
            }
        }.orEmpty()
        return RingRules.isRepeat(number, nowMillis, earlier)
    }
}
