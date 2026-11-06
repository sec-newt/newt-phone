package io.github.secnewt.dialer.calls

import org.junit.Assert.assertEquals
import org.junit.Test

class DialpadTest {

    @Test
    fun `typing, backspace and hold-zero-for-plus`() {
        var number = ""
        for (c in "555") number = Dialpad.press(number, c)
        assertEquals("555", number)
        assertEquals("55", Dialpad.backspace(number))
        assertEquals("+", Dialpad.longPress("", '0'))
        assertEquals("5", Dialpad.longPress("", '5'))
        assertEquals("", Dialpad.backspace(""))
    }

    @Test
    fun `numbers stop at the maximum length`() {
        val full = "1".repeat(Dialpad.MAX_LENGTH)
        assertEquals(full, Dialpad.press(full, '2'))
    }

    @Test
    fun `pasted text keeps only what can be dialed`() {
        assertEquals("+15550197731", Dialpad.clean("tel: +1 (555) 019-7731"))
        assertEquals("5550100,123#", Dialpad.clean("555-0100 ext,123#"))
    }

    @Test
    fun `keys read clearly to a screen reader`() {
        assertEquals("5, J K L", Dialpad.spokenLabel(DialKey('5', "JKL")))
        assertEquals("1, hold for voicemail", Dialpad.spokenLabel(DialKey('1', "")))
        assertEquals("3, D E F", Dialpad.spokenLabel(DialKey('3', "DEF")))
        assertEquals("Star", Dialpad.spokenLabel(DialKey('*', "")))
        assertEquals("0, hold for plus", Dialpad.spokenLabel(DialKey('0', "+")))
    }

    @Test
    fun `the pad has twelve keys in phone order`() {
        assertEquals("123456789*0#", Dialpad.keys.map { it.digit }.joinToString(""))
    }

    @Test
    fun `holding 1 calls voicemail only when nothing is typed`() {
        assertEquals(true, Dialpad.holdCallsVoicemail("", '1'))
        assertEquals(false, Dialpad.holdCallsVoicemail("555", '1'))
        assertEquals(false, Dialpad.holdCallsVoicemail("", '2'))
    }

    @Test
    fun `keys type at the cursor and replace a selection`() {
        assertEquals(DialText("5550197", 4), Dialpad.type(DialText("555197", 3), "0"))
        assertEquals(DialText("5559", 4), Dialpad.type(DialText("555123", 3, 6), "9"))
        // A backwards selection works the same.
        assertEquals(DialText("5559", 4), Dialpad.type(DialText("555123", 6, 3), "9"))
        assertEquals(DialText("+1", 1), Dialpad.type(DialText("1", 0), "+"))
    }

    @Test
    fun `typing at the cursor stops at the maximum length`() {
        val full = DialText("1".repeat(Dialpad.MAX_LENGTH), 5)
        assertEquals(full, Dialpad.type(full, "2"))
        // Replacing a selection makes room.
        assertEquals("2" + "1".repeat(Dialpad.MAX_LENGTH - 1), Dialpad.type(DialText(full.text, 0, 1), "2").text)
    }

    @Test
    fun `delete removes the selection or the digit before the cursor`() {
        assertEquals(DialText("55597", 3), Dialpad.backspace(DialText("555097", 4)))
        assertEquals(DialText("55597", 3), Dialpad.backspace(DialText("5550197", 3, 5)))
        assertEquals(DialText("555", 0), Dialpad.backspace(DialText("555", 0)))
        assertEquals(DialText("", 0), Dialpad.backspace(DialText("")))
    }

    @Test
    fun `pasting through the system bar keeps only digits and the cursor in place`() {
        // "555" + pasted "(972) 01" + "77", cursor after the paste.
        val edit = Dialpad.cleanEdit("555(972) 0177", 11, 11)
        assertEquals(DialText("5559720177", 8), edit)
    }

    @Test
    fun `formatted positions follow the typed digits`() {
        assertEquals(listOf(1, 2, 3, 6, 7), Dialpad.formatPositions("55501", "(555) 01")!!.toList())
        assertEquals(listOf(0, 1, 2, 4), Dialpad.formatPositions("5550", "555-0")!!.toList())
        assertEquals(emptyList<Int>(), Dialpad.formatPositions("", "")!!.toList())
        // A formatter that adds or changes digits isn't trusted.
        assertEquals(null, Dialpad.formatPositions("5550", "1 555-0"))
        assertEquals(null, Dialpad.formatPositions("5550", "555"))
    }

    @Test
    fun `pasted tones keep digits, star and pound only`() {
        assertEquals("4242424242424242", Dialpad.toneDigits("4242 4242-4242 4242"))
        assertEquals("1234#", Dialpad.toneDigits("PIN: 1234#"))
        assertEquals("", Dialpad.toneDigits("no digits here"))
    }

    @Test
    fun `the send question shows only the last four of long numbers`() {
        assertEquals("Send 16 digits ending in 4242?", Dialpad.tonePrompt("4111111111114242"))
        assertEquals("Send 1234?", Dialpad.tonePrompt("1234"))
        assertEquals("Send the digit 5?", Dialpad.tonePrompt("5"))
    }
}
