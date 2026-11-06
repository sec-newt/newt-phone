package io.github.secnewt.dialer.calls

/**
 * The typed number and where the cursor is. When [start] and [end] differ, that part is
 * selected (either order: dragging backwards puts [start] after [end]).
 */
data class DialText(val text: String, val start: Int = text.length, val end: Int = start) {
    val from: Int get() = minOf(start, end).coerceIn(0, text.length)
    val to: Int get() = maxOf(start, end).coerceIn(0, text.length)
}

/** A key on the dialpad: the digit and the letters printed under it. */
data class DialKey(val digit: Char, val letters: String)

/** The dialpad's keys and editing rules, kept free of Android so they can be tested. */
object Dialpad {

    val keys = listOf(
        DialKey('1', ""), DialKey('2', "ABC"), DialKey('3', "DEF"),
        DialKey('4', "GHI"), DialKey('5', "JKL"), DialKey('6', "MNO"),
        DialKey('7', "PQRS"), DialKey('8', "TUV"), DialKey('9', "WXYZ"),
        DialKey('*', ""), DialKey('0', "+"), DialKey('#', ""),
    )

    /** Longest number the dialpad accepts; real numbers, extensions included, fit easily. */
    const val MAX_LENGTH = 32

    fun press(current: String, key: Char): String =
        if (current.length >= MAX_LENGTH) current else current + key

    /** Holding 0 types "+" for international numbers. */
    fun longPress(current: String, key: Char): String =
        if (key == '0') press(current, '+') else press(current, key)

    /** Holding 1 with nothing typed calls voicemail, like the stock dialer. */
    fun holdCallsVoicemail(current: String, key: Char): Boolean = key == '1' && current.isEmpty()

    fun backspace(current: String): String = current.dropLast(1)

    /** Types at the cursor, replacing anything selected. Stops at [MAX_LENGTH]. */
    fun type(current: DialText, chars: String): DialText {
        val room = MAX_LENGTH - (current.text.length - (current.to - current.from))
        if (room <= 0) return current
        val typed = chars.take(room)
        val text = current.text.substring(0, current.from) + typed + current.text.substring(current.to)
        return DialText(text, current.from + typed.length)
    }

    /** Deletes what's selected, or the character just before the cursor. */
    fun backspace(current: DialText): DialText = when {
        current.from != current.to ->
            DialText(current.text.removeRange(current.from, current.to), current.from)
        current.from == 0 -> DialText(current.text, 0)
        else -> DialText(current.text.removeRange(current.from - 1, current.from), current.from - 1)
    }

    /**
     * An edit from the system's Cut and Paste bar: keeps only what can be dialed, and keeps the
     * cursor next to the same characters it was next to.
     */
    fun cleanEdit(text: String, start: Int, end: Int): DialText {
        val cleaned = clean(text)
        fun at(i: Int) = clean(text.take(i.coerceIn(0, text.length))).length.coerceAtMost(cleaned.length)
        return DialText(cleaned, at(start), at(end))
    }

    /**
     * Where each typed character sits in its formatted version ("5550197" in "555-0197").
     * Null when the formatted text doesn't contain the typed characters in order, so the
     * caller can show the number as typed instead.
     */
    fun formatPositions(typed: String, formatted: String): IntArray? {
        val positions = IntArray(typed.length)
        var f = 0
        typed.forEachIndexed { i, c ->
            while (f < formatted.length && formatted[f] != c) {
                if (formatted[f].isLetterOrDigit() || formatted[f] in "+*#,;") return null
                f++
            }
            if (f == formatted.length) return null
            positions[i] = f++
        }
        return positions
    }

    /** The digits a pasted text sends as tones during a call ("4242 4242-4242" → "424242424242"). */
    fun toneDigits(text: String): String = text.filter { it in '0'..'9' || it == '*' || it == '#' }

    /** The question before sending pasted tones; long ones show only the last four. */
    fun tonePrompt(digits: String): String = when {
        digits.length > 4 -> "Send ${digits.length} digits ending in ${digits.takeLast(4)}?"
        digits.length == 1 -> "Send the digit $digits?"
        else -> "Send $digits?"
    }

    /** Keeps only what can be dialed, for text pasted or shared into the dialpad. */
    fun clean(text: String): String =
        text.filter { it.isDigit() || it in "+*#," }.take(MAX_LENGTH)

    /** What a screen reader says for a key: "5, J K L". */
    fun spokenLabel(key: DialKey): String = when {
        key.digit == '*' -> "Star"
        key.digit == '#' -> "Pound"
        key.digit == '0' -> "0, hold for plus"
        key.digit == '1' -> "1, hold for voicemail"
        key.letters.isEmpty() -> key.digit.toString()
        else -> "${key.digit}, ${key.letters.toList().joinToString(" ")}"
    }

    /** What a screen reader says for a key while in a call, where keys only send tones. */
    fun toneLabel(key: DialKey): String = when (key.digit) {
        '*' -> "Star"
        '#' -> "Pound"
        else -> key.digit.toString()
    }
}
