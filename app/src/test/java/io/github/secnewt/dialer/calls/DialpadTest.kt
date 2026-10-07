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
}
