package io.github.secnewt.dialer.screening

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The built-in numbering list (from NANPA, see scripts/build-exchanges.py). */
class ExchangesTest {

    private val bytes = File("src/main/assets/${Exchanges.ASSET}").readBytes()
    private val exchanges = checkNotNull(Exchanges.parse(bytes))

    @Test
    fun `real exchanges are not made up`() {
        // Oct 15 spam and the one real call: all from exchanges a company has.
        listOf("9723600633", "9728880427", "9729709217", "8084325359", "3073171463", "+1 (972) 787-3200").forEach {
            assertFalse(it, exchanges.isMadeUp(it))
        }
    }

    @Test
    fun `exchanges nobody has are made up`() {
        assertTrue(exchanges.isMadeUp("9725550123")) // 555 is never a real line
        assertTrue(exchanges.isMadeUp("9721234567")) // exchanges can't start with 1
        assertTrue(exchanges.isMadeUp("9729111234")) // N11 codes are services, not lines
    }

    @Test
    fun `numbers outside the list are never called made up`() {
        assertFalse(exchanges.isMadeUp("8005550100")) // toll-free isn't in the list
        assertFalse(exchanges.isMadeUp("4165550100")) // Toronto: Canada isn't covered
        assertFalse(exchanges.isMadeUp("12345"))
        assertFalse(exchanges.isMadeUp(null))
    }

    @Test
    fun `the data is recent enough`() {
        assertTrue("data date ${exchanges.dataDate}", exchanges.dataDate >= 20261001)
    }

    @Test
    fun `a damaged file is refused`() {
        val damaged = bytes.copyOf().also { it[5000] = (it[5000] + 1).toByte() }
        assertNull(Exchanges.parse(damaged))
        assertNull(Exchanges.parse(bytes.copyOf(bytes.size - 1)))
        assertNull(Exchanges.parse(ByteArray(0)))
        assertNotNull(Exchanges.parse(bytes))
        assertEquals(bytes.size, 125_145)
    }
}
