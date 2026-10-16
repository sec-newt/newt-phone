package io.github.secnewt.dialer.screening

/** Why a call got the action it did, shown in the recent calls list. */
enum class Reason { NONE, BLOCK_LIST, HIDDEN_NUMBER, COPYCAT, FAILED_VERIFICATION, NEW_NUMBER }

data class ScreeningDecision(val action: CallAction, val reason: Reason)

/** What the phone already knows about a caller, read from contacts and the call log. */
data class CallerHistory(
    val inContacts: Boolean = false,
    /** I've called this number before. */
    val calledBefore: Boolean = false,
    /** The same number called within the last few minutes. */
    val repeatCaller: Boolean = false,
    /**
     * False when contacts or the call log couldn't be read. Then nobody counts as a new
     * number: without the history, everyone would look new.
     */
    val known: Boolean = false,
)

/**
 * Decides what to do with an incoming call.
 *
 * Checks run in a fixed order and the first match wins:
 * hidden number, my block list, a contact (rings), copycat of my number, failed carrier
 * verification, a new number. A number I've called before is never a copycat or a new
 * number, and a second call within a few minutes is never a new number, so a real person
 * who tries again gets through.
 */
object CallScreener {

    fun decide(
        number: String?,
        verification: Verification,
        settings: SpamSettings,
        blockRules: List<BlockRule>,
        history: CallerHistory = CallerHistory(),
    ): ScreeningDecision {
        if (PhoneNumbers.normalize(number) == null) {
            return ScreeningDecision(settings.hiddenNumbers, Reason.HIDDEN_NUMBER)
        }
        if (BlockList.match(blockRules, number) != null) {
            return ScreeningDecision(CallAction.BLOCK, Reason.BLOCK_LIST)
        }
        // Android usually keeps contacts' calls away from screening; this makes sure.
        if (history.inContacts) {
            return ScreeningDecision(CallAction.RING, Reason.NONE)
        }
        if (!history.calledBefore && PhoneNumbers.looksLikeCopycat(settings.myNumber, number)) {
            return ScreeningDecision(settings.copycatNumbers, Reason.COPYCAT)
        }
        if (verification == Verification.FAILED) {
            return ScreeningDecision(settings.likelySpam, Reason.FAILED_VERIFICATION)
        }
        if (history.known && !history.calledBefore && !history.repeatCaller) {
            return ScreeningDecision(settings.newNumbers, Reason.NEW_NUMBER)
        }
        return ScreeningDecision(CallAction.RING, Reason.NONE)
    }
}
