package io.github.secnewt.dialer.share

import io.github.secnewt.dialer.screening.BlockRule
import org.junit.Assert.assertEquals
import org.junit.Test

class BlockRulesProviderTest {

    @Test
    fun `only starting-with rules are shared`() {
        val rules = listOf(
            BlockRule.Number("+19725550111"),
            BlockRule.StartsWith("800555"),
            BlockRule.StartsWith(""),
            BlockRule.StartsWith("800555"),
            BlockRule.StartsWith("1844"),
        )
        assertEquals(listOf("800555", "1844"), BlockRulesProvider.startsWith(rules))
    }
}
