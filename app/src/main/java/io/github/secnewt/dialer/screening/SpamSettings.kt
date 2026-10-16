package io.github.secnewt.dialer.screening

/** What happens to a call. Kept to three plain words on purpose. */
enum class CallAction { RING, SILENCE, BLOCK }

/** The one-tap presets. CUSTOM means the rows were changed by hand. */
enum class ProtectionLevel { OFF, BALANCED, STRICT, CUSTOM }

data class SpamSettings(
    val level: ProtectionLevel = ProtectionLevel.BALANCED,
    /** Calls that failed carrier verification (and later, other spam signals). */
    val likelySpam: CallAction = CallAction.SILENCE,
    val hiddenNumbers: CallAction = CallAction.SILENCE,
    /** Calls that copy the start of my own number. */
    val copycatNumbers: CallAction = CallAction.SILENCE,
    /**
     * New numbers: not in my contacts and never called by me. Set to Ring while I'm expecting
     * calls from people I don't know yet, like during a job search.
     */
    val newNumbers: CallAction = CallAction.SILENCE,
    /** Made-up numbers: their exchange belongs to no phone company, so the caller ID is fake. */
    val fakeNumbers: CallAction = CallAction.SILENCE,
    /**
     * For an hour after I call a number that isn't a contact, new numbers ring: businesses
     * often call back from a different number than the one I dialed.
     */
    val ringAfterICall: Boolean = true,
    /** Until this time (epoch millis), new numbers ring: I'm expecting a call. 0 when off. */
    val expectingUntil: Long = 0L,
    /** When on, every call rings and the app only records what it would have done. */
    val observeOnly: Boolean = true,
    /** Used for the copycat check. Typed in by hand; never read from the SIM. */
    val myNumber: String? = null,
) {
    /** Applies a preset. Choosing CUSTOM keeps the current rows. */
    fun withLevel(newLevel: ProtectionLevel): SpamSettings {
        val preset = PRESETS[newLevel] ?: return copy(level = ProtectionLevel.CUSTOM)
        return copy(
            level = newLevel,
            likelySpam = preset.likelySpam,
            hiddenNumbers = preset.hiddenNumbers,
            copycatNumbers = preset.copycatNumbers,
            newNumbers = preset.newNumbers,
            fakeNumbers = preset.fakeNumbers,
        )
    }

    fun withLikelySpam(action: CallAction) = copy(likelySpam = action).withMatchingLevel()
    fun withHiddenNumbers(action: CallAction) = copy(hiddenNumbers = action).withMatchingLevel()
    fun withCopycatNumbers(action: CallAction) = copy(copycatNumbers = action).withMatchingLevel()
    fun withNewNumbers(action: CallAction) = copy(newNumbers = action).withMatchingLevel()
    fun withFakeNumbers(action: CallAction) = copy(fakeNumbers = action).withMatchingLevel()

    /** Whether I'm expecting a call at [nowMillis]. */
    fun expecting(nowMillis: Long): Boolean = nowMillis < expectingUntil

    /** After a row changes, show the preset it now matches, or "Custom". */
    private fun withMatchingLevel(): SpamSettings {
        val match = PRESETS.entries.firstOrNull { (_, p) ->
            p.likelySpam == likelySpam && p.hiddenNumbers == hiddenNumbers && p.copycatNumbers == copycatNumbers &&
                p.newNumbers == newNumbers && p.fakeNumbers == fakeNumbers
        }?.key
        return copy(level = match ?: ProtectionLevel.CUSTOM)
    }

    private data class Preset(
        val likelySpam: CallAction,
        val hiddenNumbers: CallAction,
        val copycatNumbers: CallAction,
        val newNumbers: CallAction,
        val fakeNumbers: CallAction,
    )

    private companion object {
        val PRESETS = mapOf(
            ProtectionLevel.OFF to Preset(CallAction.RING, CallAction.RING, CallAction.RING, CallAction.RING, CallAction.RING),
            ProtectionLevel.BALANCED to
                Preset(CallAction.SILENCE, CallAction.SILENCE, CallAction.SILENCE, CallAction.SILENCE, CallAction.SILENCE),
            // New numbers are only silenced, even here: a real first call should still reach voicemail.
            ProtectionLevel.STRICT to
                Preset(CallAction.BLOCK, CallAction.SILENCE, CallAction.BLOCK, CallAction.SILENCE, CallAction.BLOCK),
        )
    }
}
