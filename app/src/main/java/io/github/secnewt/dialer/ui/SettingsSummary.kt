package io.github.secnewt.dialer.ui

import io.github.secnewt.dialer.announce.AnnounceMode
import io.github.secnewt.dialer.announce.AnnounceSettings
import io.github.secnewt.dialer.calls.RingSettings
import io.github.secnewt.dialer.screening.CallAction
import io.github.secnewt.dialer.screening.ProtectionLevel
import io.github.secnewt.dialer.screening.SpamSettings

/**
 * The "Right now" sentence at the top of Settings: what the phone does with calls, in plain
 * words. [roleHeld] is whether this app screens calls (null when unknown: counted as yes).
 */
fun settingsSummary(spam: SpamSettings, ring: RingSettings, announce: AnnounceSettings, roleHeld: Boolean?): String {
    val ringing = when {
        ring.starredRingThrough && ring.repeatRingThrough -> "Favorites and repeat callers ring on silent."
        ring.starredRingThrough -> "Favorites ring on silent."
        ring.repeatRingThrough -> "Repeat callers ring on silent."
        else -> "Nobody rings on silent."
    }
    val actions = listOf(spam.likelySpam, spam.hiddenNumbers, spam.copycatNumbers, spam.newNumbers, spam.fakeNumbers)
    val spamLine = when {
        roleHeld == false || spam.level == ProtectionLevel.OFF -> "Spam protection is off."
        spam.observeOnly -> "Spam is watched, not blocked."
        CallAction.BLOCK in actions -> "Suspicious calls are blocked or silenced."
        else -> "Suspicious calls are silenced."
    }
    val announceLine = if (announce.mode != AnnounceMode.OFF) " Callers are announced." else ""
    return "$ringing $spamLine$announceLine"
}
