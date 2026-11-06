package io.github.secnewt.dialer.incall

import kotlinx.coroutines.delay

/**
 * Sends pasted digits as tones, one at a time, like a person pressing keys: each tone is held
 * for [TONE_MS], with a [GAP_MS] pause before the next. Automated phone menus can miss tones
 * that come faster than that. Cancelling stops at once, and never leaves a tone playing.
 */
object ToneSender {
    const val TONE_MS = 250L
    const val GAP_MS = 250L

    suspend fun send(
        digits: String,
        start: (Char) -> Unit,
        stop: () -> Unit,
        onSent: (Int) -> Unit = {},
        pause: suspend (Long) -> Unit = { delay(it) },
    ) {
        digits.forEachIndexed { index, digit ->
            if (index > 0) pause(GAP_MS)
            start(digit)
            try {
                pause(TONE_MS)
            } finally {
                stop()
            }
            onSent(index + 1)
        }
    }
}
