package io.github.secnewt.dialer.screening

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SilencedCallsTest {

    private val at = 1_000_000L

    @Before
    fun reset() = SilencedCalls.clear()

    @Test
    fun sameNumberMomentsLaterMatches() {
        assertTrue(SilencedCalls.matches("2145550123", at, "+1 (214) 555-0123", at + 500))
    }

    @Test
    fun differentNumberDoesNotMatch() {
        assertFalse(SilencedCalls.matches("2145550123", at, "4695550175", at + 500))
    }

    @Test
    fun hiddenNumberMatchesHiddenNumber() {
        assertTrue(SilencedCalls.matches(null, at, null, at + 500))
        assertFalse(SilencedCalls.matches(null, at, "4695550175", at + 500))
    }

    @Test
    fun oldDecisionDoesNotMatch() {
        assertFalse(SilencedCalls.matches("2145550123", at, "2145550123", at + SilencedCalls.MATCH_MILLIS + 1))
        assertFalse(SilencedCalls.matches("2145550123", at, "2145550123", at - 1))
    }

    @Test
    fun aDecisionIsUsedOnlyOnce() {
        SilencedCalls.mark("2145550123", at)
        assertTrue(SilencedCalls.take("2145550123", at + 100))
        assertFalse(SilencedCalls.take("2145550123", at + 200))
    }

    @Test
    fun aCallThatWasNotSilencedLeavesTheRecord() {
        SilencedCalls.mark("2145550123", at)
        assertFalse(SilencedCalls.take("4695550175", at + 100))
        assertTrue(SilencedCalls.take("2145550123", at + 200))
    }

    @Test
    fun twoSilencedCallsAtOnceEachMatchOnce() {
        SilencedCalls.mark("2145550123", at)
        SilencedCalls.mark("4695550175", at + 100)
        assertTrue(SilencedCalls.take("2145550123", at + 200))
        assertTrue(SilencedCalls.take("4695550175", at + 300))
        assertFalse(SilencedCalls.take("2145550123", at + 400))
        assertFalse(SilencedCalls.take("4695550175", at + 500))
    }

    @Test
    fun expiredDecisionsAreDropped() {
        SilencedCalls.mark("2145550123", at)
        SilencedCalls.mark("4695550175", at + SilencedCalls.MATCH_MILLIS + 1)
        assertFalse(SilencedCalls.take("2145550123", at + SilencedCalls.MATCH_MILLIS + 2))
    }

    @Test
    fun aLateScreeningAnswerIsNotRemembered() {
        assertTrue(SilencedCalls.answeredInTime(0))
        assertTrue(SilencedCalls.answeredInTime(SilencedCalls.SCREENING_DEADLINE_MILLIS))
        assertFalse(SilencedCalls.answeredInTime(SilencedCalls.SCREENING_DEADLINE_MILLIS + 1))
        assertFalse(SilencedCalls.answeredInTime(5_000))
    }
}
