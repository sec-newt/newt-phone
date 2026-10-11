package io.github.secnewt.dialer.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissedCallCountTest {

    @Test
    fun `only the count path answers`() {
        assertTrue(MissedCallCount.isCountPath("/count"))
        assertFalse(MissedCallCount.isCountPath("/calls"))
        assertFalse(MissedCallCount.isCountPath("/count/1"))
        assertFalse(MissedCallCount.isCountPath(null))
    }

    @Test
    fun `counts stay sensible`() {
        assertEquals(0, MissedCallCount.clamp(-3))
        assertEquals(2, MissedCallCount.clamp(2))
        assertEquals(999, MissedCallCount.clamp(5000))
    }
}
