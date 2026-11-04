package io.github.secnewt.dialer.ui

import io.github.secnewt.dialer.announce.AnnounceMode
import io.github.secnewt.dialer.announce.AnnounceSettings
import io.github.secnewt.dialer.calls.RingSettings
import io.github.secnewt.dialer.screening.CallAction
import io.github.secnewt.dialer.screening.ProtectionLevel
import io.github.secnewt.dialer.screening.SpamSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsSummaryTest {

    private val watching = SpamSettings(observeOnly = true)
    private fun ring(starred: Boolean, repeat: Boolean) = RingSettings(starredRingThrough = starred, repeatRingThrough = repeat)

    @Test
    fun defaultsReadAsTwoSentences() = assertEquals(
        "Favorites and repeat callers ring on silent. Spam is watched, not blocked.",
        settingsSummary(watching, RingSettings(), AnnounceSettings(), roleHeld = true),
    )

    @Test
    fun ringingSentences() {
        assertEquals("Favorites ring on silent.", settingsSummary(watching, ring(true, false), AnnounceSettings(), true).substringBefore(" Spam"))
        assertEquals("Repeat callers ring on silent.", settingsSummary(watching, ring(false, true), AnnounceSettings(), true).substringBefore(" Spam"))
        assertEquals("Nobody rings on silent.", settingsSummary(watching, ring(false, false), AnnounceSettings(), true).substringBefore(" Spam"))
    }

    @Test
    fun spamOffWhenNotTheSpamApp() = assertEquals(
        "Favorites and repeat callers ring on silent. Spam protection is off.",
        settingsSummary(watching, RingSettings(), AnnounceSettings(), roleHeld = false),
    )

    @Test
    fun spamOffAtLevelOff() = assertEquals(
        "Favorites and repeat callers ring on silent. Spam protection is off.",
        settingsSummary(SpamSettings(observeOnly = false).withLevel(ProtectionLevel.OFF), RingSettings(), AnnounceSettings(), true),
    )

    @Test
    fun spamSilencedOrBlocked() {
        val silencing = SpamSettings(observeOnly = false)
        assertEquals(
            "Favorites and repeat callers ring on silent. Suspicious calls are silenced.",
            settingsSummary(silencing, RingSettings(), AnnounceSettings(), true),
        )
        assertEquals(
            "Favorites and repeat callers ring on silent. Suspicious calls are blocked or silenced.",
            settingsSummary(silencing.withHiddenNumbers(CallAction.BLOCK), RingSettings(), AnnounceSettings(), true),
        )
    }

    @Test
    fun announceAddsASentence() = assertEquals(
        "Favorites and repeat callers ring on silent. Spam is watched, not blocked. Callers are announced.",
        settingsSummary(watching, RingSettings(), AnnounceSettings(mode = AnnounceMode.ALWAYS), true),
    )

    @Test
    fun unknownRoleCountsAsHeld() = assertEquals(
        "Favorites and repeat callers ring on silent. Spam is watched, not blocked.",
        settingsSummary(watching, RingSettings(), AnnounceSettings(), roleHeld = null),
    )
}
