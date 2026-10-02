package io.github.secnewt.dialer.screening

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ScreeningLogTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun call(time: Long, number: String? = "5550197731") =
        ScreenedCall(time, number, Verification.NONE, Decision.ALLOWED)

    @Test
    fun `starts empty`() {
        assertTrue(ScreeningLog(File(folder.root, "log.json")).read().isEmpty())
    }

    @Test
    fun `newest call comes first`() {
        val log = ScreeningLog(File(folder.root, "log.json"))
        log.add(call(1))
        log.add(call(2))
        assertEquals(listOf(2L, 1L), log.read().map { it.timeMillis })
    }

    @Test
    fun `keeps every field, including hidden numbers`() {
        val log = ScreeningLog(File(folder.root, "log.json"))
        val verified = ScreenedCall(5, "+15550142290", Verification.PASSED, Decision.ALLOWED)
        val hidden = ScreenedCall(6, null, Verification.FAILED, Decision.ALLOWED)
        log.add(verified)
        log.add(hidden)
        assertEquals(listOf(hidden, verified), log.read())
    }

    @Test
    fun `survives being reopened`() {
        val file = File(folder.root, "log.json")
        ScreeningLog(file).add(call(7))
        assertEquals(listOf(7L), ScreeningLog(file).read().map { it.timeMillis })
    }

    @Test
    fun `drops the oldest calls past the limit`() {
        val log = ScreeningLog(File(folder.root, "log.json"), maxEntries = 3)
        (1L..5L).forEach { log.add(call(it)) }
        assertEquals(listOf(5L, 4L, 3L), log.read().map { it.timeMillis })
    }

    @Test
    fun `a damaged file reads as empty instead of crashing`() {
        val file = File(folder.root, "log.json").apply { writeText("not json") }
        assertTrue(ScreeningLog(file).read().isEmpty())
    }
}
