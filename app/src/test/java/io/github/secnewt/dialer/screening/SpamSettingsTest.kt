package io.github.secnewt.dialer.screening

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpamSettingsTest {

    @Test
    fun `new installs start balanced and observe-only`() {
        val settings = SpamSettings()
        assertEquals(ProtectionLevel.BALANCED, settings.level)
        assertTrue(settings.observeOnly)
    }

    @Test
    fun `presets set all three rows`() {
        val strict = SpamSettings().withLevel(ProtectionLevel.STRICT)
        assertEquals(CallAction.BLOCK, strict.likelySpam)
        assertEquals(CallAction.SILENCE, strict.hiddenNumbers)
        assertEquals(CallAction.BLOCK, strict.copycatNumbers)

        val off = strict.withLevel(ProtectionLevel.OFF)
        assertEquals(listOf(CallAction.RING, CallAction.RING, CallAction.RING), listOf(off.likelySpam, off.hiddenNumbers, off.copycatNumbers))
    }

    @Test
    fun `changing a row by hand switches the level to custom`() {
        val changed = SpamSettings().withHiddenNumbers(CallAction.RING)
        assertEquals(ProtectionLevel.CUSTOM, changed.level)
        assertEquals(CallAction.RING, changed.hiddenNumbers)
    }

    @Test
    fun `changing rows back to a preset shows that preset again`() {
        val back = SpamSettings().withHiddenNumbers(CallAction.RING).withHiddenNumbers(CallAction.SILENCE)
        assertEquals(ProtectionLevel.BALANCED, back.level)
    }

    @Test
    fun `presets keep observe-only and my number`() {
        val settings = SpamSettings(observeOnly = false, myNumber = "5550142290").withLevel(ProtectionLevel.STRICT)
        assertEquals(false, settings.observeOnly)
        assertEquals("5550142290", settings.myNumber)
    }
}
