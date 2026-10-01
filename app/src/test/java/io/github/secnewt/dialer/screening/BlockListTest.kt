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
}
