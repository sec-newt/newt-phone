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
        )
    }

    fun withLikelySpam(action: CallAction) = copy(likelySpam = action).withMatchingLevel()
    fun withHiddenNumbers(action: CallAction) = copy(hiddenNumbers = action).withMatchingLevel()
    fun withCopycatNumbers(action: CallAction) = copy(copycatNumbers = action).withMatchingLevel()

    /** After a row changes, show the preset it now matches, or "Custom". */
    private fun withMatchingLevel(): SpamSettings {
        val match = PRESETS.entries.firstOrNull { (_, p) ->
            p.likelySpam == likelySpam && p.hiddenNumbers == hiddenNumbers && p.copycatNumbers == copycatNumbers
        }?.key
        return copy(level = match ?: ProtectionLevel.CUSTOM)
    }

    private data class Preset(
        val likelySpam: CallAction,
        val hiddenNumbers: CallAction,
        val copycatNumbers: CallAction,
    )

    private companion object {
        val PRESETS = mapOf(
            ProtectionLevel.OFF to Preset(CallAction.RING, CallAction.RING, CallAction.RING),
            ProtectionLevel.BALANCED to Preset(CallAction.SILENCE, CallAction.SILENCE, CallAction.SILENCE),
            ProtectionLevel.STRICT to Preset(CallAction.BLOCK, CallAction.SILENCE, CallAction.BLOCK),
        )
    }
}
