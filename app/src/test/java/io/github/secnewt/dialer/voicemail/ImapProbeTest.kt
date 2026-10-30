package io.github.secnewt.dialer.voicemail

import java.io.BufferedReader
import java.io.StringReader
import java.io.StringWriter
import java.net.ConnectException
import java.net.SocketException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImapProbeTest {

    private fun run(server: String, user: String = "u1", pw: String = "p\"w"): Pair<ProbeResult, String> {
        val out = StringWriter()
        return probeMailbox(BufferedReader(StringReader(server)), out, user, pw) to out.toString()
    }

    @Test
    fun `logs in and counts messages`() {
        val (r, sent) = run("* OK ready\r\na1 OK logged in\r\n* 3 EXISTS\r\n* 0 RECENT\r\na2 OK selected\r\n* BYE\r\na3 OK\r\n")
        assertEquals(ProbeResult.LoggedIn(3), r)
        assertTrue(sent.startsWith("a1 LOGIN \"u1\" \"p\\\"w\"\r\na2 SELECT INBOX\r\n"))
    }

    @Test
    fun `server hanging up after logout still counts`() {
        val (r, _) = run("* OK ready\r\na1 OK\r\n* 1 EXISTS\r\na2 OK\r\n* BYE\r\n")
        assertEquals(ProbeResult.LoggedIn(1), r)
    }

    @Test
    fun `refused login keeps the server text but not the password`() {
        val (r, _) = run("* OK ready\r\na1 NO [AUTHENTICATIONFAILED] Invalid credentials\r\n")
        assertEquals(ProbeResult.Refused("[AUTHENTICATIONFAILED] Invalid credentials"), r)
        assertFalse(probeLine(r).contains("p\"w"))
    }

    @Test
    fun `connection closed early fails plainly`() = assertTrue(run("* OK ready\r\n").first is ProbeResult.Failed)

    @Test
    fun `permission denied socket error explains the Network permission`() =
        assertEquals(VvmText.NETWORK_PERMISSION, connectFailureLine(SocketException("socket failed: EACCES (Permission denied)")))

    @Test
    fun `other socket errors give the class name only`() =
        assertEquals(VvmText.failed("ConnectException"), connectFailureLine(ConnectException("10.1.2.3:143 user1@ting refused")))
}
