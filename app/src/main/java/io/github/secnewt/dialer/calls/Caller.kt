package io.github.secnewt.dialer.calls

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.TelecomManager
import android.telephony.TelephonyManager

/** How a call was started, so the screen can say what happened. */
enum class CallStart { PLACED, OPENED_PHONE_APP, FAILED }

/** Places calls. Emergency numbers always go through the phone's own dialer. */
class Caller(private val context: Context) {

    fun canCallDirectly(): Boolean =
        context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    fun call(number: String): CallStart {
        val uri = Uri.fromParts("tel", number, null)
        if (!isEmergency(number) && canCallDirectly()) {
            try {
                context.getSystemService(TelecomManager::class.java).placeCall(uri, null)
                return CallStart.PLACED
            } catch (e: SecurityException) {
                // Permission was just turned off: fall back to the phone app below.
            }
        }
        return try {
            context.startActivity(Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            CallStart.OPENED_PHONE_APP
        } catch (e: ActivityNotFoundException) {
            CallStart.FAILED
        }
    }

    /**
     * 911 and other emergency numbers are handed to the phone's own dialer, which is built and
     * tested for them (location, priority routing). It shows the number; one tap places the call.
     */
    private fun isEmergency(number: String): Boolean = try {
        context.getSystemService(TelephonyManager::class.java).isEmergencyNumber(number)
    } catch (e: Exception) {
        number.filter { it.isDigit() } in setOf("911", "112", "999", "000", "110", "119")
    }
}
