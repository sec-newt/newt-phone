package io.github.secnewt.dialer.screening

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpamLabelTest {

    private val now = 1_800_000_000_000L

    @Test
    fun blockListMatchSaysBlocked() {
        val log = listOf(ScreenedCall(now, "+1 555-019-7731", Verification.NONE, CallAction.BLOCK, Reason.BLOCK_LIST))
        assertEquals("Blocked number", SpamLabel.forCall("5550197731", now + 5_000, log))
    }

    @Test
    fun failedVerificationSaysLikelySpam() {
        val log = listOf(ScreenedCall(now, "5550197731", Verification.FAILED))
        assertEquals("Likely spam", SpamLabel.forCall("5550197731", now, log))
    }

    @Test
    fun aCallThatSimplyRangHasNoLabel() {
        val log = listOf(ScreenedCall(now, "5550197731", Verification.PASSED))
        assertNull(SpamLabel.forCall("5550197731", now, log))
    }

    @Test
    fun anOldScreeningOfTheSameNumberDoesNotCount() {
        val log = listOf(ScreenedCall(now - 10 * 60_000, "5550197731", Verification.FAILED))
        assertNull(SpamLabel.forCall("5550197731", now, log))
    }

    @Test
    fun hiddenNumbersHaveNoLabel() {
        assertNull(SpamLabel.forCall(null, now, listOf(ScreenedCall(now, null, Verification.FAILED))))
    }
}
