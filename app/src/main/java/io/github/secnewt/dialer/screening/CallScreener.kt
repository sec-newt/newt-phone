package io.github.secnewt.dialer.screening

/** Why a call got the action it did, shown in the recent calls list. */
enum class Reason { NONE, BLOCK_LIST, HIDDEN_NUMBER, COPYCAT, FAILED_VERIFICATION }

data class ScreeningDecision(val action: CallAction, val reason: Reason)

/**
 * Decides what to do with an incoming call from someone not in my contacts.
 * (Android never sends calls from contacts to a screening app, so contacts always ring.)
 *
 * Checks run in a fixed order and the first match wins:
 * hidden number, my block list, copycat of my number, failed carrier verification.
 */
object CallScreener {

    fun decide(
        number: String?,
        verification: Verification,
        settings: SpamSettings,
        blockRules: List<BlockRule>,
    ): ScreeningDecision {
        if (PhoneNumbers.normalize(number) == null) {
            return ScreeningDecision(settings.hiddenNumbers, Reason.HIDDEN_NUMBER)
        }
        if (BlockList.match(blockRules, number) != null) {
            return ScreeningDecision(CallAction.BLOCK, Reason.BLOCK_LIST)
        }
        if (PhoneNumbers.looksLikeCopycat(settings.myNumber, number)) {
            return ScreeningDecision(settings.copycatNumbers, Reason.COPYCAT)
        }
        if (verification == Verification.FAILED) {
            return ScreeningDecision(settings.likelySpam, Reason.FAILED_VERIFICATION)
        }
        return ScreeningDecision(CallAction.RING, Reason.NONE)
    }
}
