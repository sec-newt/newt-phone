package io.github.secnewt.dialer.calls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RingRulesTest {

    private val settings = RingSettings(quietHours = QuietHours(enabled = true, startMinutes = 22 * 60, endMinutes = 7 * 60))
    private val noon = 12 * 60
    private val midnight = 0

    private fun call(
        audible: Boolean = true,
        starred: Boolean = false,
        repeat: Boolean = false,
        flagged: Boolean = false,
        at: Int = noon,
    ) = RingSituation(audible, starred, repeat, flagged, at)

    @Test
    fun starredContactsRingThroughSilent() {
        assertEquals(RingAction.RING_THROUGH, RingRules.decide(settings, call(audible = false, starred = true)))
    }

    @Test
    fun repeatCallersRingThroughSilent() {
        assertEquals(RingAction.RING_THROUGH, RingRules.decide(settings, call(audible = false, repeat = true)))
    }

    @Test
    fun everyoneElseStaysSilentOnSilent() {
        assertEquals(RingAction.NORMAL, RingRules.decide(settings, call(audible = false)))
    }

    @Test
    fun withTheRingerOnOutsideQuietHoursAndroidRings() {
        assertEquals(RingAction.NORMAL, RingRules.decide(settings, call(starred = true)))
        assertEquals(RingAction.NORMAL, RingRules.decide(settings, call()))
    }

    @Test
    fun quietHoursSilenceEveryoneButSpecialCallers() {
        assertEquals(RingAction.SILENCE, RingRules.decide(settings, call(at = midnight)))
        assertEquals(RingAction.NORMAL, RingRules.decide(settings, call(at = midnight, starred = true)))
        assertEquals(RingAction.NORMAL, RingRules.decide(settings, call(at = midnight, repeat = true)))
    }

    @Test
    fun spamNeverGetsThrough() {
        assertEquals(RingAction.NORMAL, RingRules.decide(settings, call(audible = false, repeat = true, flagged = true)))
    }

    @Test
    fun switchesTurnEachRuleOff() {
        val off = RingSettings(starredRingThrough = false, repeatRingThrough = false)
        assertEquals(RingAction.NORMAL, RingRules.decide(off, call(audible = false, starred = true, repeat = true)))
        assertEquals(RingAction.NORMAL, RingRules.decide(off, call(at = midnight)))
    }

    @Test
    fun quietHoursCanWrapPastMidnight() {
        val hours = settings.quietHours
        assertTrue(RingRules.inQuietHours(hours, 23 * 60))
        assertTrue(RingRules.inQuietHours(hours, 6 * 60 + 59))
        assertFalse(RingRules.inQuietHours(hours, 7 * 60))
        assertFalse(RingRules.inQuietHours(hours, 21 * 60 + 59))
    }

    @Test
    fun quietHoursWithinOneDay() {
        val hours = QuietHours(enabled = true, startMinutes = 13 * 60, endMinutes = 15 * 60)
        assertTrue(RingRules.inQuietHours(hours, 14 * 60))
        assertFalse(RingRules.inQuietHours(hours, 15 * 60))
        assertFalse(RingRules.inQuietHours(hours.copy(enabled = false), 14 * 60))
    }

    @Test
    fun aSecondCallWithinThreeMinutesIsARepeat() {
        val now = 1_800_000_000_000L
        val earlier = listOf<Pair<String?, Long>>("+1 555-019-7731" to now - 90_000)
        assertTrue(RingRules.isRepeat("5550197731", now, earlier))
        assertFalse(RingRules.isRepeat("5550197731", now, listOf<Pair<String?, Long>>("5550197731" to now - 4 * 60_000)))
        assertFalse(RingRules.isRepeat("5550142290", now, earlier))
        assertFalse(RingRules.isRepeat(null, now, earlier))
    }

    @Test
    fun ringAndVoiceTakeTurnsWhenThePhoneRingsOutLoud() {
        assertTrue(RingRules.takeTurns(isPhoneApp = true, callRingingHere = true, ringerAudible = true, ringingThrough = false))
    }

    @Test
    fun ringAndVoiceTakeTurnsWhileRingingThroughSilent() {
        assertTrue(RingRules.takeTurns(isPhoneApp = true, callRingingHere = true, ringerAudible = false, ringingThrough = true))
    }

    @Test
    fun noTurnsOnASilentPhone() {
        assertFalse(RingRules.takeTurns(isPhoneApp = true, callRingingHere = true, ringerAudible = false, ringingThrough = false))
    }

    @Test
    fun noTurnsUnlessThisIsThePhoneAppAndKnowsTheCall() {
        assertFalse(RingRules.takeTurns(isPhoneApp = false, callRingingHere = true, ringerAudible = true, ringingThrough = false))
        assertFalse(RingRules.takeTurns(isPhoneApp = true, callRingingHere = false, ringerAudible = true, ringingThrough = false))
    }
}
