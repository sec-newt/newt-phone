package io.github.secnewt.dialer.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CallFormattingTest {

    private val zone = ZoneId.of("America/Chicago")
    private val now = ZonedDateTime.of(2026, 10, 1, 18, 0, 0, 0, zone)

    private fun millis(day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `calls from today say Today`() {
        assertEquals("Today, 3:42 PM".normalizeSpaces(), formatCallTime(millis(1, 15, 42), now).normalizeSpaces())
    }

    @Test
    fun `calls from yesterday say Yesterday`() {
        val yesterday = ZonedDateTime.of(2026, 9, 30, 9, 5, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals("Yesterday, 9:05 AM", formatCallTime(yesterday, now).normalizeSpaces())
    }

    @Test
    fun `older calls show the weekday and date`() {
        val older = ZonedDateTime.of(2026, 9, 28, 15, 42, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals("Mon, Sep 28, 3:42 PM", formatCallTime(older, now).normalizeSpaces())
    }

    @Test
    fun `US numbers are formatted`() {
        assertEquals("(555) 019-7731", formatCaller("5550197731", "US"))
    }

    @Test
    fun `hidden numbers say so`() {
        assertEquals("Hidden number", formatCaller(null))
        assertEquals("Hidden number", formatCaller(""))
    }

    // Newer Java versions put a narrow no-break space before AM/PM.
    private fun String.normalizeSpaces() = replace(' ', ' ').replace(' ', ' ')
}
