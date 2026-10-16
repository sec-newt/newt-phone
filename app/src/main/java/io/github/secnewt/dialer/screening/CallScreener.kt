package io.github.secnewt.dialer.screening

/** Why a call got the action it did, shown in the recent calls list. */
enum class Reason { NONE, BLOCK_LIST, HIDDEN_NUMBER, COPYCAT, FAILED_VERIFICATION, NEW_NUMBER, FAKE_NUMBER }

data class ScreeningDecision(val action: CallAction, val reason: Reason)

/** What the phone already knows about a caller, read from contacts and the call log. */
data class CallerHistory(
    val inContacts: Boolean = false,
    /** I've called this number before. */
    val calledBefore: Boolean = false,
    /** The same number called within the last few minutes. */
    val repeatCaller: Boolean = false,
    /** I called a number that isn't a contact within the last hour. */
    val calledSomeoneNewRecently: Boolean = false,
    /** I called 911 (or another emergency number) within the last hour. */
    val emergencyCallRecently: Boolean = false,
    /** The number's exchange belongs to no phone company: it was made up. */
    val madeUp: Boolean = false,
    /**
     * False when contacts or the call log couldn't be read. Then nobody counts as a new
     * number: without the history, everyone would look new.
     */
    val known: Boolean = false,
)

/**
 * Decides what to do with an incoming call.
 *
 * After an emergency call, everything rings for an hour, so the emergency service can always
 * call back. Otherwise checks run in a fixed order and the first match wins:
 * hidden number, my block list, a contact (rings), a made-up number, copycat of my number,
 * failed carrier verification, a new number.
 *
 * A number I've called before is never made up, a copycat or new. New numbers also ring when
 * they call twice within a few minutes, for an hour after I call someone new (if that's on),
 * and while I'm expecting a call.
 */
object CallScreener {

    fun decide(
        number: String?,
        verification: Verification,
        settings: SpamSettings,
        blockRules: List<BlockRule>,
        history: CallerHistory = CallerHistory(),
        nowMillis: Long = 0L,
    ): ScreeningDecision {
        if (history.emergencyCallRecently) {
            return ScreeningDecision(CallAction.RING, Reason.NONE)
        }
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
        if (!history.calledBefore && history.madeUp) {
            return ScreeningDecision(settings.fakeNumbers, Reason.FAKE_NUMBER)
        }
        if (!history.calledBefore && PhoneNumbers.looksLikeCopycat(settings.myNumber, number)) {
            return ScreeningDecision(settings.copycatNumbers, Reason.COPYCAT)
        }
        if (verification == Verification.FAILED) {
            return ScreeningDecision(settings.likelySpam, Reason.FAILED_VERIFICATION)
        }
        val letThrough = history.calledBefore || history.repeatCaller ||
            (settings.ringAfterICall && history.calledSomeoneNewRecently) || settings.expecting(nowMillis)
        if (history.known && !letThrough) {
            return ScreeningDecision(settings.newNumbers, Reason.NEW_NUMBER)
        }
        return ScreeningDecision(CallAction.RING, Reason.NONE)
    }
}
