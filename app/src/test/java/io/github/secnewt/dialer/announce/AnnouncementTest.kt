package io.github.secnewt.dialer.announce

import io.github.secnewt.dialer.screening.CallAction
import io.github.secnewt.dialer.screening.Reason
import io.github.secnewt.dialer.screening.ScreenedCall
import io.github.secnewt.dialer.screening.Verification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnouncementTest {

    private val always = AnnounceSettings(mode = AnnounceMode.ALWAYS)
    private val headphonesOnly = AnnounceSettings(mode = AnnounceMode.HEADPHONES_ONLY)
    private val speaker = AudioSituation(headphonesConnected = false, ringerAudible = true, doNotDisturbOn = false)
    private val headphones = speaker.copy(headphonesConnected = true)
    private val silentPhone = speaker.copy(ringerAudible = false)
    private val dnd = speaker.copy(doNotDisturbOn = true)

    private fun call(action: CallAction, reason: Reason, enforced: Boolean) =
        ScreenedCall(1, "5550197731", Verification.NONE, action, reason, enforced)

    @Test
    fun `off never speaks`() {
        assertFalse(Announcement.shouldSpeak(AnnounceSettings(), headphones, null))
    }

    @Test
    fun `always speaks out loud when the ringer is on`() {
        assertTrue(Announcement.shouldSpeak(always, speaker, null))
    }

    @Test
    fun `never speaks out loud on a silenced phone, but headphones are fine`() {
        assertFalse(Announcement.shouldSpeak(always, silentPhone, null))
        assertTrue(Announcement.shouldSpeak(always, silentPhone.copy(headphonesConnected = true), null))
    }

    @Test
    fun `headphones-only mode needs headphones`() {
        assertFalse(Announcement.shouldSpeak(headphonesOnly, speaker, null))
        assertTrue(Announcement.shouldSpeak(headphonesOnly, headphones, null))
    }

    @Test
    fun `quiet during Do Not Disturb unless turned off`() {
        assertFalse(Announcement.shouldSpeak(always, dnd, null))
        assertTrue(Announcement.shouldSpeak(always.copy(quietDuringDnd = false), dnd, null))
    }

    @Test
    fun `silenced or blocked calls are not announced`() {
        assertFalse(Announcement.shouldSpeak(always, speaker, call(CallAction.SILENCE, Reason.HIDDEN_NUMBER, enforced = true)))
        assertFalse(Announcement.shouldSpeak(always, speaker, call(CallAction.BLOCK, Reason.BLOCK_LIST, enforced = true)))
    }

    @Test
    fun `observe-only calls still ring, so they are announced`() {
        assertTrue(Announcement.shouldSpeak(always, speaker, call(CallAction.SILENCE, Reason.FAILED_VERIFICATION, enforced = false)))
    }

    @Test
    fun `contacts are announced by name`() {
        assertEquals("Call from Mom", Announcement.text("Mom", "5550142290", null))
    }

    @Test
    fun `unknown numbers are read digit by digit`() {
        assertEquals("Call from 5 5 5, 0 1 9, 7 7 3 1", Announcement.text(null, "+1 (555) 019-7731", null))
    }

    @Test
    fun `hidden numbers say so`() {
        assertEquals("Call from a private number", Announcement.text(null, null, null))
        assertEquals("Call from a private number", Announcement.text(null, "", null))
    }

    @Test
    fun `flagged callers get a warning first`() {
        assertEquals(
            "Likely spam, from 5 5 5, 0 1 9, 7 7 3 1",
            Announcement.text(null, "5550197731", call(CallAction.SILENCE, Reason.FAILED_VERIFICATION, enforced = false)),
        )
        assertEquals(
            "Blocked number, from 5 5 5, 0 1 9, 7 7 3 1",
            Announcement.text(null, "5550197731", call(CallAction.BLOCK, Reason.BLOCK_LIST, enforced = false)),
        )
    }

    @Test
    fun `other number lengths are read in groups of four`() {
        assertEquals("4 4 2 0, 7 1 2 3, 4 5 6 7", Announcement.spellOut("442071234567"))
    }

    @Test
    fun `a calling app with no number is not a private phone call`() {
        assertTrue(Announcement.isAppCall(null, phoneCallRinging = false))
        assertTrue(Announcement.isAppCall("", phoneCallRinging = false))
    }

    @Test
    fun `a real phone call with a hidden number is still announced`() {
        assertFalse(Announcement.isAppCall(null, phoneCallRinging = true))
        assertFalse(Announcement.isAppCall("", phoneCallRinging = true))
    }

    @Test
    fun `a call with a number is never treated as an app call`() {
        assertFalse(Announcement.isAppCall("5550197731", phoneCallRinging = false))
        assertFalse(Announcement.isAppCall("5550197731", phoneCallRinging = true))
    }
}
