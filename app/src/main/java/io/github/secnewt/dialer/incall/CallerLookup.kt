package io.github.secnewt.dialer.incall

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
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
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
                val columns = arrayOf(PhoneLookup.DISPLAY_NAME, PhoneLookup.PHOTO_THUMBNAIL_URI)
                context.contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(0)
                        photo = cursor.getString(1)
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
        return CallerInfo(name = name, photoUri = photo, spamLabel = spam, isEmergency = emergency)
    }
}
