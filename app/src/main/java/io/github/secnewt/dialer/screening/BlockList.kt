package io.github.secnewt.dialer.screening

/** One entry on my block list. Kept deliberately simple: no wildcards, no regex. */
sealed interface BlockRule {
    /** Blocks exactly this number. */
    data class Number(val number: String) : BlockRule

    /** Blocks every number starting with these digits, e.g. "800555". */
    data class StartsWith(val digits: String) : BlockRule
}

object BlockList {

    /** Returns the first rule that matches [caller], or null if the call is not blocked. */
    fun match(rules: List<BlockRule>, caller: String?): BlockRule? {
        val number = PhoneNumbers.normalize(caller) ?: return null
        return rules.firstOrNull { rule ->
            when (rule) {
                is BlockRule.Number -> PhoneNumbers.normalize(rule.number) == number
                is BlockRule.StartsWith -> {
                    val prefix = PhoneNumbers.normalize(rule.digits)
                    prefix != null && number.startsWith(prefix)
                }
            }
        }
    }
}
