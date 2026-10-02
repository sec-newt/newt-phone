package io.github.secnewt.dialer.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import android.util.Log

/**
 * Sees every incoming call before it rings, once the app holds the
 * "Caller ID & spam app" role.
 *
 * For now it allows every call and records it in the recent calls list,
 * including whether the carrier verified the caller ID. The block list and
 * spam rules come next.
 */
class SpamScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        respondToCall(callDetails, CallResponse.Builder().build())
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return

        val verification = when (callDetails.callerNumberVerificationStatus) {
            Connection.VERIFICATION_STATUS_PASSED -> Verification.PASSED
            Connection.VERIFICATION_STATUS_FAILED -> Verification.FAILED
            else -> Verification.NONE
        }
        ScreeningLog(this).add(
            ScreenedCall(
                timeMillis = System.currentTimeMillis(),
                number = callDetails.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() },
                verification = verification,
                decision = Decision.ALLOWED,
            )
        )
        // Never log the phone number itself to the system log.
        Log.i(TAG, "Incoming call, carrier verification: ${verification.name.lowercase()}")
    }

    private companion object {
        const val TAG = "SpamScreening"
    }
}
