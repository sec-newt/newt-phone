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
