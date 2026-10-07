package io.github.secnewt.dialer.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.secnewt.dialer.screening.BlockRule
import io.github.secnewt.dialer.screening.CallAction
import io.github.secnewt.dialer.screening.Reason
import io.github.secnewt.dialer.screening.ScreenedCall
import io.github.secnewt.dialer.screening.Verification
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
        assertEquals("(972) 862-8342", formatCaller("+1 972-862-8342", "US"))
        assertEquals("(972) 862-8342", formatCaller("+19728628342", "US"))
    }

    @Test
    fun `hidden numbers say so`() {
        assertEquals("Hidden number", formatCaller(null))
        assertEquals("Hidden number", formatCaller(""))
    }

    @Test
    fun `calls that rang get no outcome label`() {
        assertEquals(null, outcomeLabel(ScreenedCall(1, "5550197731", Verification.NONE)))
    }

    @Test
    fun `observe-only calls say what would have happened`() {
        val call = ScreenedCall(1, null, Verification.NONE, CallAction.SILENCE, Reason.HIDDEN_NUMBER, enforced = false)
        assertEquals("Would silence: hidden number", outcomeLabel(call))
    }

    @Test
    fun `enforced calls say what happened`() {
        val call = ScreenedCall(1, "5550197731", Verification.NONE, CallAction.BLOCK, Reason.BLOCK_LIST, enforced = true)
        assertEquals("Blocked: on your block list", outcomeLabel(call))
    }

    @Test
    fun `block rules read plainly`() {
        assertEquals("(555) 019-7731", ruleLabel(BlockRule.Number("5550197731")))
        assertEquals("Numbers starting with 800555", ruleLabel(BlockRule.StartsWith("800555")))
    }

    // Newer Java versions put a narrow no-break space before AM/PM.
    private fun String.normalizeSpaces() = replace(' ', ' ').replace(' ', ' ')
}
