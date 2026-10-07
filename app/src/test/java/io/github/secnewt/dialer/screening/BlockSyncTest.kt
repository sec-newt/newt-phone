package io.github.secnewt.dialer.screening

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockSyncTest {

    @Test
    fun copiesMissingNumbersEachWay() {
        val rules = listOf(BlockRule.Number("5550197731"), BlockRule.Number("5550142290"))
        val android = listOf("+1 555-019-7731", "8005550000")
        val plan = BlockSync.plan(rules, android)
        assertEquals(listOf("5550142290"), plan.toAndroid)
        assertEquals(listOf("8005550000"), plan.toApp)
    }

    @Test
    fun startsWithRulesStayInTheApp() {
        val plan = BlockSync.plan(listOf(BlockRule.StartsWith("800555")), emptyList())
        assertEquals(emptyList<String>(), plan.toAndroid)
    }

    @Test
    fun sameNumberWrittenTwoWaysIsCopiedOnce() {
        val plan = BlockSync.plan(emptyList(), listOf("5550197731", "+15550197731"))
        assertEquals(listOf("5550197731"), plan.toApp)
    }

    @Test
    fun alreadyInSyncMeansNothingToDo() {
        val plan = BlockSync.plan(listOf(BlockRule.Number("(555) 019-7731")), listOf("5550197731"))
        assertEquals(BlockSync.Plan(emptyList(), emptyList()), plan)
    }

    @Test
    fun changeListsAddedAndRemovedExactNumbers() {
        val before = listOf(BlockRule.Number("5550197731"), BlockRule.StartsWith("800"))
        val after = listOf(BlockRule.Number("5550142290"), BlockRule.StartsWith("800"))
        val change = BlockSync.change(before, after)
        assertEquals(listOf("5550142290"), change.added)
        assertEquals(listOf("5550197731"), change.removed)
    }

    @Test
    fun reformattingANumberIsNotAChange() {
        val change = BlockSync.change(listOf(BlockRule.Number("5550197731")), listOf(BlockRule.Number("+1 555 019 7731")))
        assertEquals(BlockSync.Change(emptyList(), emptyList()), change)
    }
}
