package io.github.secnewt.dialer.calls

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

    fun backspace(current: String): String = current.dropLast(1)

    /** Keeps only what can be dialed, for text pasted or shared into the dialpad. */
    fun clean(text: String): String =
        text.filter { it.isDigit() || it in "+*#," }.take(MAX_LENGTH)

    /** What a screen reader says for a key: "5, J K L". */
    fun spokenLabel(key: DialKey): String = when {
        key.digit == '*' -> "Star"
        key.digit == '#' -> "Pound"
        key.digit == '0' -> "0, hold for plus"
        key.letters.isEmpty() -> key.digit.toString()
        else -> "${key.digit}, ${key.letters.toList().joinToString(" ")}"
    }
}
