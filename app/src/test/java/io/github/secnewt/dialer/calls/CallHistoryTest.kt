package io.github.secnewt.dialer.calls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class CallHistoryTest {

    private val zone = ZoneId.of("America/Chicago")
    private fun at(day: Int, hour: Int) = ZonedDateTime.of(2026, 10, day, hour, 0, 0, 0, zone).toInstant().toEpochMilli()

    private fun call(id: Long, number: String?, kind: CallKind, time: Long, seconds: Long = 0) =
        LoggedCall(id, number, null, kind, time, seconds)

    @Test
    fun `back-to-back calls from the same number on one day are one row`() {
        val calls = listOf(
            call(1, "5550197731", CallKind.MISSED, at(6, 15)),
            call(2, "+1 (555) 019-7731", CallKind.MISSED, at(6, 14)),
            call(3, "5550142290", CallKind.OUTGOING, at(6, 13)),
            call(4, "5550197731", CallKind.INCOMING, at(6, 12)),
        )
        val groups = CallHistory.group(calls, zone)
        assertEquals(listOf(2, 1, 1), groups.map { it.count })
        assertEquals(1L, groups[0].latest.id)
    }

    @Test
    fun `the same number on different days is separate rows`() {
        val calls = listOf(
            call(1, "5550197731", CallKind.MISSED, at(6, 9)),
            call(2, "5550197731", CallKind.MISSED, at(5, 22)),
        )
        assertEquals(2, CallHistory.group(calls, zone).size)
    }

    @Test
    fun `hidden numbers group with each other`() {
        val calls = listOf(call(1, null, CallKind.MISSED, at(6, 9)), call(2, "", CallKind.MISSED, at(6, 8)))
        assertEquals(1, CallHistory.group(calls, zone).size)
    }

    @Test
    fun `summary says what happened and how long or how many`() {
        val one = CallGroup(listOf(call(1, "1", CallKind.OUTGOING, at(6, 9), seconds = 250)))
        val missed = CallGroup(listOf(call(1, "1", CallKind.MISSED, at(6, 9))))
        val three = CallGroup(List(3) { call(it.toLong(), "1", CallKind.INCOMING, at(6, 9), 30) })
        assertEquals("Outgoing, 4 minutes", CallHistory.summary(one))
        assertEquals("Missed", CallHistory.summary(missed))
        assertEquals("Incoming, 3 calls", CallHistory.summary(three))
    }

    @Test
    fun `durations read naturally`() {
        assertNull(CallHistory.duration(0))
        assertEquals("45 seconds", CallHistory.duration(45))
        assertEquals("1 minute", CallHistory.duration(61))
        assertEquals("1 hour", CallHistory.duration(3600))
        assertEquals("1 hour 1 minute", CallHistory.duration(3660))
        assertEquals("2 hours 5 minutes", CallHistory.duration(7500))
    }
}
