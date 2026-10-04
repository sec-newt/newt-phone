package io.github.secnewt.dialer.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import android.util.Log

/**
 * Sees every incoming call from someone not in my contacts before it rings,
 * once the app holds the "Caller ID & spam app" role.
 *
 * It applies the spam rules and the block list. In observe-only mode (the
 * default) every call still rings and the decision is only recorded.
 */
class SpamScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val number = callDetails.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() }
        val verification = when (callDetails.callerNumberVerificationStatus) {
            Connection.VERIFICATION_STATUS_PASSED -> Verification.PASSED
            Connection.VERIFICATION_STATUS_FAILED -> Verification.FAILED
            else -> Verification.NONE
        }

        // If anything goes wrong while deciding, the call rings: never lose a real call to a bug.
        val (decision, enforced) = try {
            val store = SpamSettingsStore(this)
            val settings = store.settings()
            CallScreener.decide(number, verification, settings, store.blockRules()) to !settings.observeOnly
        } catch (e: Exception) {
            Log.w(TAG, "Screening failed, letting the call ring", e)
            ScreeningDecision(CallAction.RING, Reason.NONE) to false
        }

        respondToCall(callDetails, responseFor(if (enforced) decision.action else CallAction.RING))

        ScreeningLog(this).add(
            ScreenedCall(
                timeMillis = System.currentTimeMillis(),
                number = number,
                verification = verification,
                action = decision.action,
                reason = decision.reason,
                enforced = enforced,
            )
        )
        // Never log the phone number itself to the system log.
        Log.i(
            TAG,
            "Incoming call, carrier verification: ${verification.name.lowercase()}, " +
                "decision: ${decision.action.name.lowercase()} (${decision.reason.name.lowercase()}), " +
                "enforced: $enforced",
        )
    }

    private fun responseFor(action: CallAction): CallResponse = when (action) {
        CallAction.RING -> CallResponse.Builder().build()
        CallAction.SILENCE -> CallResponse.Builder().setSilenceCall(true).build()
        // Rejected calls still appear in the call log and blocked-calls list, so mistakes are visible.
        CallAction.BLOCK -> CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
    }

    private companion object {
        const val TAG = "SpamScreening"
    }
}
