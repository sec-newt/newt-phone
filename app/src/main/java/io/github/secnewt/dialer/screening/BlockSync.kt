package io.github.secnewt.dialer.screening

/**
 * Keeps exact numbers on my block list and Android's own block list the same. Android's list
 * only holds exact numbers, so "starting with" rules stay in this app only.
 */
object BlockSync {

    /** What to copy each way so both lists end up with the same exact numbers. */
    data class Plan(val toAndroid: List<String>, val toApp: List<String>)

    fun plan(rules: List<BlockRule>, androidNumbers: List<String>): Plan {
        val appNumbers = rules.filterIsInstance<BlockRule.Number>().map { it.number }
        val appKeys = appNumbers.mapNotNull(PhoneNumbers::normalize).toSet()
        val androidKeys = androidNumbers.mapNotNull(PhoneNumbers::normalize).toSet()
        return Plan(
            toAndroid = appNumbers.filter { PhoneNumbers.normalize(it).let { key -> key != null && key !in androidKeys } }
                .distinctBy(PhoneNumbers::normalize),
            toApp = androidNumbers.filter { PhoneNumbers.normalize(it).let { key -> key != null && key !in appKeys } }
                .distinctBy(PhoneNumbers::normalize),
        )
    }

    /** Exact numbers added and removed between two versions of my block list. */
    data class Change(val added: List<String>, val removed: List<String>)

    fun change(before: List<BlockRule>, after: List<BlockRule>): Change {
        fun numbers(rules: List<BlockRule>) = rules.filterIsInstance<BlockRule.Number>().map { it.number }
        val beforeKeys = numbers(before).mapNotNull(PhoneNumbers::normalize).toSet()
        val afterKeys = numbers(after).mapNotNull(PhoneNumbers::normalize).toSet()
        return Change(
            added = numbers(after).filter { PhoneNumbers.normalize(it) !in beforeKeys },
            removed = numbers(before).filter { PhoneNumbers.normalize(it) !in afterKeys },
        )
    }
}
