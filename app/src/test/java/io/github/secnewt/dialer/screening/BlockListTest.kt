package io.github.secnewt.dialer.screening

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlockListTest {

    private val exact = BlockRule.Number("(555) 019-7731")
    private val tollFree = BlockRule.StartsWith("800555")
    private val rules = listOf(exact, tollFree)

    @Test
    fun `exact number matches regardless of formatting`() {
        assertEquals(exact, BlockList.match(rules, "+1 555-019-7731"))
    }

    @Test
    fun `starts-with rule matches numbers with that prefix`() {
        assertEquals(tollFree, BlockList.match(rules, "1-800-555-0000"))
    }

    @Test
    fun `other numbers are not blocked`() {
        assertNull(BlockList.match(rules, "5550142290"))
        assertNull(BlockList.match(rules, "8005560000"))
    }

    @Test
    fun `hidden numbers are never matched by the block list`() {
        assertNull(BlockList.match(rules, null))
        assertNull(BlockList.match(rules, ""))
    }

    @Test
    fun `empty block list blocks nothing`() {
        assertNull(BlockList.match(emptyList(), "5550197731"))
    }

    @Test
    fun `starts-with rule typed with the country code still matches`() {
        val dallas = BlockRule.StartsWith("+1 972")
        assertEquals(dallas, BlockList.match(listOf(dallas), "+19725550123"))
        assertEquals(dallas, BlockList.match(listOf(dallas), "972-555-0123"))
        val withOne = BlockRule.StartsWith("1800555")
        assertEquals(withOne, BlockList.match(listOf(withOne), "8005550000"))
        assertNull(BlockList.match(listOf(dallas), "2145550123"))
    }

    @Test
    fun `a lone 1 never turns into a rule that blocks everyone`() {
        assertNull(BlockList.match(listOf(BlockRule.StartsWith("1")), "9725550123"))
    }
}
