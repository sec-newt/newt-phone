package io.github.secnewt.dialer.screening

object PhoneNumbers {

    /**
     * Digits only, with a leading US/Canada country code removed, so
     * "+1 (555) 014-2290" and "555-014-2290" compare equal.
     * Returns null for hidden or empty numbers.
     */
    fun normalize(raw: String?): String? {
        val digits = raw?.filter { it.isDigit() }.orEmpty()
        if (digits.isEmpty()) return null
        return if (digits.length == 11 && digits.startsWith("1")) digits.substring(1) else digits
    }

    /**
     * True when a caller copies the start of my own number ("neighbor spoofing"),
     * or spoofs my exact number. Only applies to 10-digit US/Canada numbers.
     */
    fun looksLikeCopycat(myNumber: String?, caller: String?, prefixLength: Int = 6): Boolean {
        val mine = normalize(myNumber) ?: return false
        val theirs = normalize(caller) ?: return false
        if (mine.length != 10 || theirs.length != 10) return false
        return mine.take(prefixLength) == theirs.take(prefixLength)
    }
}
