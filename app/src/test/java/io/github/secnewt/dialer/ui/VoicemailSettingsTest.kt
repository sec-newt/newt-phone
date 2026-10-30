package io.github.secnewt.dialer.ui

import io.github.secnewt.dialer.voicemail.StatusLine
import io.github.secnewt.dialer.voicemail.VvmText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoicemailSettingsTest {

    private val sentAt = 1_000_000L
    private val twoMinutes = 2 * 60 * 1000L

    @Test
    fun `nothing to say with no steps`() = assertNull(waitingHint(emptyList(), sentAt))

    @Test
    fun `quiet while the text is recent`() =
        assertNull(waitingHint(listOf(StatusLine(sentAt, VvmText.STATUS_SENT)), sentAt + twoMinutes))

    @Test
    fun `says Ting hasn't answered after 2 minutes`() {
        assertEquals(VvmText.WAITING, waitingHint(listOf(StatusLine(sentAt, VvmText.STATUS_SENT)), sentAt + twoMinutes + 1))
        assertEquals(VvmText.WAITING, waitingHint(listOf(StatusLine(sentAt, VvmText.ACTIVATE_SENT)), sentAt + twoMinutes + 1))
    }

    @Test
    fun `quiet once Ting has answered`() = assertNull(
        waitingHint(listOf(StatusLine(sentAt, VvmText.STATUS_SENT), StatusLine(sentAt + 5, VvmText.loggedIn(3))), sentAt + 10 * twoMinutes),
    )
}
