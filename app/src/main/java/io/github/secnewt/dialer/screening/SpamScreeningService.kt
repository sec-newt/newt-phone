package io.github.secnewt.dialer.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import android.util.Log

/**
 * Sees every incoming call before it rings, once the app holds the
 * "Caller ID & spam app" role.
 *
 * Skeleton for now: it allows every call and logs whether the carrier verified
 * the caller ID, so we can find out what Ting actually passes through.
 * The block list and spam rules come next.
 */
class SpamScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection == Call.Details.DIRECTION_INCOMING) {
            // Never log the phone number itself.
            Log.i(TAG, "Incoming call, carrier verification: " +
                describeVerification(callDetails.callerNumberVerificationStatus))
        }
        respondToCall(callDetails, CallResponse.Builder().build())
    }

    private fun describeVerification(status: Int): String = when (status) {
        Connection.VERIFICATION_STATUS_PASSED -> "passed"
        Connection.VERIFICATION_STATUS_FAILED -> "failed"
        else -> "not verified"
    }

    private companion object {
        const val TAG = "SpamScreening"
    }
}
