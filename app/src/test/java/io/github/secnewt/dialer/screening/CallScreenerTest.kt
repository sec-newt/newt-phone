package io.github.secnewt.dialer.screening

import org.junit.Assert.assertEquals
import org.junit.Test

class CallScreenerTest {

    private val balanced = SpamSettings(myNumber = "5550142290")
    private val strict = balanced.withLevel(ProtectionLevel.STRICT)
    private val off = balanced.withLevel(ProtectionLevel.OFF)
    private val noRules = emptyList<BlockRule>()

    private fun decide(
        number: String?,
        verification: Verification = Verification.NONE,
        settings: SpamSettings = balanced,
        rules: List<BlockRule> = noRules,
        history: CallerHistory = CallerHistory(),
    ) = CallScreener.decide(number, verification, settings, rules, history)

    // A stranger: contacts and the call log were read, and they're in neither.
    private val stranger = CallerHistory(known = true)

    @Test
    fun `a new number follows the new-numbers setting`() {
        assertEquals(ScreeningDecision(CallAction.SILENCE, Reason.NEW_NUMBER), decide("9723600633", history = stranger))
        assertEquals(CallAction.SILENCE, decide("9723600633", settings = strict, history = stranger).action)
        assertEquals(CallAction.RING, decide("9723600633", settings = off, history = stranger).action)
    }

    @Test
    fun `new numbers can be set to ring while job hunting`() {
        val expecting = balanced.withNewNumbers(CallAction.RING)
        assertEquals(CallAction.RING, decide("9723600633", settings = expecting, history = stranger).action)
    }

    @Test
    fun `a number I've called before is not new`() {
        assertEquals(ScreeningDecision(CallAction.RING, Reason.NONE), decide("3073171463", history = stranger.copy(calledBefore = true)))
    }

    @Test
    fun `a second call within minutes is not new`() {
        assertEquals(CallAction.RING, decide("9723600633", history = stranger.copy(repeatCaller = true)).action)
    }

    @Test
    fun `without the call history, nobody counts as new`() {
        assertEquals(CallAction.RING, decide("9723600633", history = CallerHistory(known = false)).action)
    }

    @Test
    fun `contacts always ring, even copycats`() {
        val contact = stranger.copy(inContacts = true)
        assertEquals(ScreeningDecision(CallAction.RING, Reason.NONE), decide("5550149999", history = contact))
    }

    @Test
    fun `a number I've called is never a copycat`() {
        assertEquals(CallAction.RING, decide("5550149999", history = stranger.copy(calledBefore = true)).action)
    }

    @Test
    fun `the block list beats being a contact`() {
        val rules = listOf<BlockRule>(BlockRule.Number("5550149999"))
        assertEquals(Reason.BLOCK_LIST, decide("5550149999", rules = rules, history = stranger.copy(inContacts = true)).reason)
    }

    @Test
    fun `made-up numbers follow their setting`() {
        val fake = stranger.copy(madeUp = true)
        assertEquals(ScreeningDecision(CallAction.SILENCE, Reason.FAKE_NUMBER), decide("2125550100", history = fake))
        assertEquals(CallAction.BLOCK, decide("2125550100", settings = strict, history = fake).action)
        assertEquals(CallAction.RING, decide("2125550100", settings = off, history = fake).action)
        // A number I've actually called exists, whatever the list says.
        assertEquals(CallAction.RING, decide("2125550100", history = fake.copy(calledBefore = true)).action)
    }

    @Test
    fun `after an emergency call everything rings, even blocked and hidden numbers`() {
        val after911 = stranger.copy(emergencyCallRecently = true, madeUp = true)
        val rules = listOf<BlockRule>(BlockRule.Number("9723600633"))
        assertEquals(ScreeningDecision(CallAction.RING, Reason.NONE), decide("9723600633", rules = rules, history = after911))
        assertEquals(CallAction.RING, decide(null, history = CallerHistory(emergencyCallRecently = true)).action)
    }

    @Test
    fun `new numbers ring for a while after I call someone new, if that's on`() {
        val callBack = stranger.copy(calledSomeoneNewRecently = true)
        assertEquals(CallAction.RING, decide("9723600633", history = callBack).action)
        assertEquals(CallAction.SILENCE, decide("9723600633", settings = balanced.copy(ringAfterICall = false), history = callBack).action)
    }

    @Test
    fun `new numbers ring while I'm expecting a call, then stop`() {
        val expecting = balanced.copy(expectingUntil = 10_000L)
        assertEquals(CallAction.RING, CallScreener.decide("9723600633", Verification.NONE, expecting, noRules, stranger, nowMillis = 9_000L).action)
        assertEquals(CallAction.SILENCE, CallScreener.decide("9723600633", Verification.NONE, expecting, noRules, stranger, nowMillis = 10_000L).action)
    }

    @Test
    fun `expecting a call doesn't let made-up numbers through`() {
        val expecting = balanced.copy(expectingUntil = 10_000L)
        val fake = stranger.copy(madeUp = true)
        assertEquals(Reason.FAKE_NUMBER, CallScreener.decide("2125550100", Verification.NONE, expecting, noRules, fake, nowMillis = 1L).reason)
    }

    @Test
    fun `hidden numbers keep their own setting`() {
        assertEquals(Reason.HIDDEN_NUMBER, decide(null, history = stranger).reason)
    }

    @Test
    fun `an ordinary unknown caller rings`() {
        assertEquals(ScreeningDecision(CallAction.RING, Reason.NONE), decide("3125550000"))
    }

    @Test
    fun `a verified caller rings`() {
        assertEquals(CallAction.RING, decide("3125550000", Verification.PASSED).action)
    }

    @Test
    fun `hidden numbers follow the hidden-number setting`() {
        assertEquals(ScreeningDecision(CallAction.SILENCE, Reason.HIDDEN_NUMBER), decide(null))
        assertEquals(ScreeningDecision(CallAction.SILENCE, Reason.HIDDEN_NUMBER), decide(""))
        assertEquals(CallAction.RING, decide(null, settings = off).action)
    }

    @Test
    fun `the block list always blocks, even when protection is off`() {
        val rules = listOf(BlockRule.Number("(312) 555-0000"))
        assertEquals(ScreeningDecision(CallAction.BLOCK, Reason.BLOCK_LIST), decide("+13125550000", settings = off, rules = rules))
    }

    @Test
    fun `starts-with block rules apply`() {
        val rules = listOf(BlockRule.StartsWith("800"))
        assertEquals(Reason.BLOCK_LIST, decide("8005551234", rules = rules).reason)
    }

    @Test
    fun `copycat numbers follow the copycat setting`() {
        assertEquals(ScreeningDecision(CallAction.SILENCE, Reason.COPYCAT), decide("5550149999"))
        assertEquals(ScreeningDecision(CallAction.BLOCK, Reason.COPYCAT), decide("5550149999", settings = strict))
    }

    @Test
    fun `no copycat check until my number is set`() {
        assertEquals(CallAction.RING, decide("5550149999", settings = SpamSettings()).action)
    }

    @Test
    fun `failed carrier verification counts as likely spam`() {
        assertEquals(ScreeningDecision(CallAction.SILENCE, Reason.FAILED_VERIFICATION), decide("3125550000", Verification.FAILED))
        assertEquals(CallAction.BLOCK, decide("3125550000", Verification.FAILED, strict).action)
        assertEquals(CallAction.RING, decide("3125550000", Verification.FAILED, off).action)
    }

    @Test
    fun `the block list wins over the other checks`() {
        val rules = listOf(BlockRule.Number("5550149999"))
        assertEquals(Reason.BLOCK_LIST, decide("5550149999", Verification.FAILED, rules = rules).reason)
    }
}
