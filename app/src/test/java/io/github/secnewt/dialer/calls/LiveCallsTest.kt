package io.github.secnewt.dialer.calls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveCallsTest {

    private fun call(id: String, phase: CallPhase, connectedAt: Long? = null) =
        LiveCall(id = id, number = "5550197731", name = null, phase = phase, connectedAtMillis = connectedAt)

    @Test
    fun telecomStatesMapToPhases() {
        assertEquals(CallPhase.RINGING, LiveCalls.phaseOf(2))
        assertEquals(CallPhase.RINGING, LiveCalls.phaseOf(13))
        assertEquals(CallPhase.DIALING, LiveCalls.phaseOf(1))
        assertEquals(CallPhase.DIALING, LiveCalls.phaseOf(9))
        assertEquals(CallPhase.ACTIVE, LiveCalls.phaseOf(4))
        assertEquals(CallPhase.ON_HOLD, LiveCalls.phaseOf(3))
        assertEquals(CallPhase.ENDED, LiveCalls.phaseOf(7))
        assertEquals(CallPhase.ENDED, LiveCalls.phaseOf(10))
    }

    @Test
    fun aRingingCallComesFirstSoItCanBeAnswered() {
        val calls = listOf(call("a", CallPhase.ACTIVE), call("b", CallPhase.RINGING))
        assertEquals("b", LiveCalls.primary(calls)?.id)
        assertEquals("a", LiveCalls.secondary(calls)?.id)
    }

    @Test
    fun theCallYouAreOnComesBeforeTheOneOnHold() {
        val calls = listOf(call("held", CallPhase.ON_HOLD), call("talking", CallPhase.ACTIVE))
        assertEquals("talking", LiveCalls.primary(calls)?.id)
        assertEquals("held", LiveCalls.secondary(calls)?.id)
    }

    @Test
    fun noCallsMeansNothingToShow() {
        assertNull(LiveCalls.primary(emptyList()))
        assertNull(LiveCalls.secondary(listOf(call("a", CallPhase.ACTIVE))))
    }

    @Test
    fun timerReadsLikeAClock() {
        assertEquals("0:00", LiveCalls.elapsed(0))
        assertEquals("0:07", LiveCalls.elapsed(7_400))
        assertEquals("12:34", LiveCalls.elapsed((12 * 60 + 34) * 1000L))
        assertEquals("1:02:03", LiveCalls.elapsed((3600 + 2 * 60 + 3) * 1000L))
        assertEquals("0:00", LiveCalls.elapsed(-500))
    }

    @Test
    fun statusLine() {
        assertEquals("Incoming call", LiveCalls.status(call("a", CallPhase.RINGING), 0))
        assertEquals("Calling…", LiveCalls.status(call("a", CallPhase.DIALING), 0))
        assertEquals("1:05", LiveCalls.status(call("a", CallPhase.ACTIVE, connectedAt = 1_000), 66_000))
        assertEquals("On hold", LiveCalls.status(call("a", CallPhase.ON_HOLD), 0))
        assertEquals("Call ended", LiveCalls.status(call("a", CallPhase.ENDED), 0))
        val emergency = call("a", CallPhase.DIALING).copy(isEmergency = true)
        assertEquals("Emergency call", LiveCalls.status(emergency, 0))
    }

    @Test
    fun withoutBluetoothTheAudioButtonTogglesTheSpeaker() {
        val audio = AudioState(route = AudioRoute.EARPIECE)
        assertFalse(LiveCalls.audioIsSwitch(audio))
        assertEquals(AudioRoute.SPEAKER, LiveCalls.nextRoute(audio))
        assertEquals(AudioRoute.EARPIECE, LiveCalls.nextRoute(audio.copy(route = AudioRoute.SPEAKER)))
    }

    @Test
    fun withAHeadsetPluggedInSpeakerOffGoesBackToTheHeadset() {
        val audio = AudioState(route = AudioRoute.SPEAKER, available = setOf(AudioRoute.WIRED, AudioRoute.SPEAKER))
        assertEquals(AudioRoute.WIRED, LiveCalls.nextRoute(audio))
    }

    @Test
    fun withBluetoothTheAudioButtonGoesAround() {
        val available = setOf(AudioRoute.EARPIECE, AudioRoute.SPEAKER, AudioRoute.BLUETOOTH)
        val audio = AudioState(route = AudioRoute.BLUETOOTH, available = available)
        assertTrue(LiveCalls.audioIsSwitch(audio))
        assertEquals(AudioRoute.EARPIECE, LiveCalls.nextRoute(audio))
        assertEquals(AudioRoute.SPEAKER, LiveCalls.nextRoute(audio.copy(route = AudioRoute.EARPIECE)))
        assertEquals(AudioRoute.BLUETOOTH, LiveCalls.nextRoute(audio.copy(route = AudioRoute.SPEAKER)))
    }

    @Test
    fun screenTurnsOffAtYourEarOnlyOnTheEarpiece() {
        val talking = listOf(call("a", CallPhase.ACTIVE))
        assertTrue(LiveCalls.wantsProximitySensor(talking, AudioState(route = AudioRoute.EARPIECE)))
        assertFalse(LiveCalls.wantsProximitySensor(talking, AudioState(route = AudioRoute.SPEAKER)))
        assertFalse(LiveCalls.wantsProximitySensor(listOf(call("a", CallPhase.RINGING)), AudioState()))
        assertFalse(LiveCalls.wantsProximitySensor(emptyList(), AudioState()))
    }
}
