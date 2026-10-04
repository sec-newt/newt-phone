package io.github.secnewt.dialer.announce

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.provider.ContactsContract.PhoneLookup
import android.telephony.TelephonyManager
import android.util.Log
import io.github.secnewt.dialer.screening.PhoneNumbers
import io.github.secnewt.dialer.screening.ScreenedCall
import io.github.secnewt.dialer.screening.ScreeningLog
import io.github.secnewt.dialer.screening.SpamSettingsStore

/**
 * Hears when the phone starts ringing (including calls from contacts, which never
 * reach the spam screening service) and announces the caller if that is turned on.
 */
class IncomingCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        if (intent.getStringExtra(TelephonyManager.EXTRA_STATE) != TelephonyManager.EXTRA_STATE_RINGING) {
            // Answered, declined or ended: stop talking right away.
            CallAnnouncer.stop()
            return
        }

        // Android sends the ringing broadcast twice; only the second one carries the number.
        if (!intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)) return
        @Suppress("DEPRECATION")
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        val settings = SpamSettingsStore(context).announceSettings()
        if (settings.mode == AnnounceMode.OFF) return

        val screening = recentScreening(context, number)
        if (!Announcement.shouldSpeak(settings, audioSituation(context), screening)) return

        val contact = contactName(context, number)
        // Never log the number or name.
        Log.i(TAG, "Announcing incoming call (contact: ${contact != null})")

        val pending = goAsync()
        CallAnnouncer.speak(context, Announcement.text(contact, number, screening)) { pending.finish() }
    }

    /** The spam decision made for this call a moment ago, if it came from an unknown number. */
    private fun recentScreening(context: Context, number: String?): ScreenedCall? {
        val target = PhoneNumbers.normalize(number)
        val cutoff = System.currentTimeMillis() - 60_000
        return ScreeningLog(context).read().firstOrNull {
            it.timeMillis >= cutoff && PhoneNumbers.normalize(it.number) == target
        }
    }

    private fun contactName(context: Context, number: String?): String? {
        if (number.isNullOrBlank()) return null
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return try {
            context.contentResolver.query(uri, arrayOf(PhoneLookup.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun audioSituation(context: Context): AudioSituation {
        val audio = context.getSystemService(AudioManager::class.java)
        val notifications = context.getSystemService(NotificationManager::class.java)
        val headphones = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in HEADPHONE_TYPES }
        return AudioSituation(
            headphonesConnected = headphones,
            ringerAudible = audio.ringerMode == AudioManager.RINGER_MODE_NORMAL,
            doNotDisturbOn = notifications.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL,
        )
    }

    private companion object {
        const val TAG = "CallAnnouncer"
        val HEADPHONE_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
        )
    }
}
