package io.github.secnewt.dialer.incall

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.PhoneLookup
import android.telephony.TelephonyManager
import io.github.secnewt.dialer.screening.ScreeningLog
import io.github.secnewt.dialer.screening.SpamLabel

/** Finds the contact, photo and spam warning for a call's number. Runs off the main thread. */
class CallerLookup(private val context: Context) {

    fun lookup(number: String?): CallerInfo {
        if (number == null) return CallerInfo()
        var name: String? = null
        var photo: String? = null
        var starred = false
        var numberType: String? = null
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
                val columns = arrayOf(
                    PhoneLookup.DISPLAY_NAME, PhoneLookup.PHOTO_THUMBNAIL_URI, PhoneLookup.STARRED,
                    PhoneLookup.TYPE, PhoneLookup.LABEL,
                )
                context.contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(0)
                        photo = cursor.getString(1)
                        starred = cursor.getInt(2) == 1
                        if (!cursor.isNull(3)) {
                            numberType = Phone.getTypeLabel(context.resources, cursor.getInt(3), cursor.getString(4))
                                .toString().lowercase()
                        }
                    }
                }
            } catch (e: Exception) {
                // No name is better than no call screen.
            }
        }
        val spam = try {
            SpamLabel.forCall(number, System.currentTimeMillis(), ScreeningLog(context).read())
        } catch (e: Exception) {
            null
        }
        val emergency = try {
            context.getSystemService(TelephonyManager::class.java).isEmergencyNumber(number)
        } catch (e: Exception) {
            false
        }
        val (talkedAt, talkedFor) = lastTalked(number)
        return CallerInfo(
            name = name, photoUri = photo, spamLabel = spam, isEmergency = emergency, starred = starred,
            numberType = numberType, lastTalkedMillis = talkedAt, lastTalkedSeconds = talkedFor,
        )
    }

    /** The latest call with this number that someone answered: when, and how long (seconds). */
    private fun lastTalked(number: String): Pair<Long?, Long?> {
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) return null to null
        return try {
            val uri = Uri.withAppendedPath(CallLog.Calls.CONTENT_FILTER_URI, Uri.encode(number))
            context.contentResolver.query(
                uri,
                arrayOf(CallLog.Calls.DATE, CallLog.Calls.DURATION),
                "${CallLog.Calls.DURATION} > 0",
                null,
                "${CallLog.Calls.DATE} DESC",
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) to cursor.getLong(1) else null } ?: (null to null)
        } catch (e: Exception) {
            null to null
        }
    }
}
