package io.github.secnewt.dialer.voicemail

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class VoicemailStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = VoicemailStore(context)

    private val reply = StatusReply.from(
        mapOf("st" to "R", "rc" to "0", "srv" to "vvm.example", "ipt" to "143", "u" to "user1@ting", "pw" to "s3cret"),
    )

    @Test
    fun `off by default`() {
        assertFalse(store.enabled)
        assertFalse(store.activateSent)
        assertNull(store.credentials())
        assertTrue(store.lines().isEmpty())
    }

    @Test
    fun `enabled survives a reload`() {
        store.enabled = true
        assertTrue(VoicemailStore(context).enabled)
    }

    @Test
    fun `credentials round trip and turning off erases them`() {
        store.enabled = true
        store.activateSent = true
        store.saveCredentials(reply)
        assertEquals(reply, VoicemailStore(context).credentials())

        store.turnOff(nowMillis = 5)
        assertFalse(store.enabled)
        assertFalse(store.activateSent)
        assertNull(store.credentials())
        assertEquals(listOf(StatusLine(5, VvmText.OFF)), store.lines())
    }

    @Test
    fun `keeps only the newest 20 lines in order`() {
        repeat(25) { store.addLine("line $it", nowMillis = it.toLong()) }
        val lines = store.lines()
        assertEquals(20, lines.size)
        assertEquals("line 5", lines.first().text)
        assertEquals("line 24", lines.last().text)
    }

    @Test
    fun `a line never shows the saved login`() {
        store.saveCredentials(reply)
        store.addLine("Ting refused the login: bad user user1@ting pass s3cret", nowMillis = 1)
        val text = store.lines().single().text
        assertFalse(text.contains("user1@ting"))
        assertFalse(text.contains("s3cret"))
        assertTrue(text.contains("(hidden)"))
    }
}
