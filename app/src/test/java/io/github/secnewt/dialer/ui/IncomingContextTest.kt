package io.github.secnewt.dialer.ui

import io.github.secnewt.dialer.calls.CallPhase
import io.github.secnewt.dialer.calls.LiveCall
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class IncomingContextTest {

    private val zone = ZoneId.of("America/Chicago")
    private val now = ZonedDateTime.of(2026, 10, 9, 18, 0, 0, 0, zone)
    private val nowMillis = now.toInstant().toEpochMilli()
    private fun ago(days: Long) = now.minusDays(days).withHour(9).toInstant().toEpochMilli()
    private val call = LiveCall(id = "1", number = "5550197731", name = "Mom", phase = CallPhase.RINGING)

    private fun lines(c: LiveCall) = incomingContext(c, nowMillis, zone)

    @Test
    fun `a favorite with a number type`() =
        assertEquals(listOf("★ Favorite · mobile"), lines(call.copy(starred = true, numberType = "mobile")))

    @Test
    fun `just the number type`() = assertEquals(listOf("mobile"), lines(call.copy(numberType = "mobile")))

    @Test
    fun `nothing known says nothing`() =
        assertEquals(emptyList<String>(), lines(call.copy(number = null, name = null)))

    @Test
    fun `last talked today yesterday weekday and date`() {
        assertEquals(listOf("Last talked today, 4\u00A0min"), lines(call.copy(lastTalkedMillis = ago(0), lastTalkedSeconds = 240)))
        assertEquals(listOf("Last talked yesterday, 4\u00A0min"), lines(call.copy(lastTalkedMillis = ago(1), lastTalkedSeconds = 250)))
        assertEquals(listOf("Last talked Tuesday, 12\u00A0min"), lines(call.copy(lastTalkedMillis = ago(3), lastTalkedSeconds = 720)))
        assertEquals(listOf("Last talked Oct 1, 2\u00A0min"), lines(call.copy(lastTalkedMillis = ago(8), lastTalkedSeconds = 120)))
    }

    @Test
    fun `a short call is under a minute`() =
        assertEquals(listOf("Last talked today, under a minute"), lines(call.copy(lastTalkedMillis = ago(0), lastTalkedSeconds = 30)))

    @Test
    fun `both lines in order`() = assertEquals(
        listOf("★ Favorite", "Last talked yesterday, 4\u00A0min"),
        lines(call.copy(starred = true, lastTalkedMillis = ago(1), lastTalkedSeconds = 240)),
    )
}
