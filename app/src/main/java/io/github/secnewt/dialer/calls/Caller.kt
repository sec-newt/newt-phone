package io.github.secnewt.dialer.calls

import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.TelecomManager
import android.telephony.TelephonyManager

/** How a call was started, so the screen can say what happened. */
enum class CallStart { PLACED, OPENED_PHONE_APP, FAILED }

/** Places calls. Emergency numbers go through the phone app, or are placed here once this is it. */
class Caller(private val context: Context) {

    fun canCallDirectly(): Boolean =
        context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    /** True once this app is the default phone app and shows calls itself. */
    fun isPhoneApp(): Boolean = try {
        context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_DIALER)
    } catch (e: Exception) {
        false
    }

    fun call(number: String): CallStart {
        val uri = Uri.fromParts("tel", number, null)
        val phoneApp = isPhoneApp()
        // As the phone app, this app is the one Android trusts with emergency calls too.
        if ((phoneApp || !isEmergency(number)) && canCallDirectly()) {
            try {
                context.getSystemService(TelecomManager::class.java).placeCall(uri, null)
                return CallStart.PLACED
            } catch (e: SecurityException) {
                // Permission was just turned off: fall back to the phone app below.
            }
        }
        // As the phone app, "open the phone app" would only come back here.
        if (phoneApp) return CallStart.FAILED
        return try {
            context.startActivity(Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            CallStart.OPENED_PHONE_APP
        } catch (e: ActivityNotFoundException) {
            CallStart.FAILED
        }
    }

    /** Calls voicemail; the phone knows the number from the SIM, so nothing needs setting up. */
    fun callVoicemail(): CallStart {
        val uri = Uri.fromParts("voicemail", "", null)
        if (canCallDirectly()) {
            try {
                context.getSystemService(TelecomManager::class.java).placeCall(uri, null)
                return CallStart.PLACED
            } catch (e: SecurityException) {
                // Fall through to the phone app.
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
     * Until this is the phone app, 911 and other emergency numbers are handed to the stock phone
     * app, which is built and tested for them. It shows the number; one tap places the call.
     */
    private fun isEmergency(number: String): Boolean = try {
        context.getSystemService(TelephonyManager::class.java).isEmergencyNumber(number)
    } catch (e: Exception) {
        number.filter { it.isDigit() } in setOf("911", "112", "999", "000", "110", "119")
    }
}
