package io.github.secnewt.dialer.incall

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ToneSenderTest {

    @Test
    fun `each digit is held, then released, with a gap before the next`() = runBlocking {
        val log = mutableListOf<String>()
        ToneSender.send(
            "12#",
            start = { log += "start $it" },
            stop = { log += "stop" },
            onSent = { log += "sent $it" },
            pause = { log += "wait $it" },
        )
        assertEquals(
            listOf(
                "start 1", "wait 250", "stop", "sent 1",
                "wait 250", "start 2", "wait 250", "stop", "sent 2",
                "wait 250", "start #", "wait 250", "stop", "sent 3",
            ),
            log,
        )
    }

    @Test
    fun `stopping mid-tone releases the tone and sends nothing more`() = runBlocking {
        val log = mutableListOf<String>()
        try {
            ToneSender.send(
                "123",
                start = { log += "start $it" },
                stop = { log += "stop" },
                pause = { if (log.lastOrNull() == "start 2") throw CancellationException("Stop") },
            )
        } catch (e: CancellationException) {
            log += "stopped"
        }
        assertEquals(listOf("start 1", "stop", "start 2", "stop", "stopped"), log)
    }
}
