package io.github.secnewt.dialer.voicemail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VvmFlowTest {

    private val cvvm = VvmConfig(VvmType.CVVM, "122", 1808, false, ConfigSource.SIM)

    private fun reply(st: String, rc: String = "0", srv: String? = "vvm.example", ipt: String? = "143") =
        StatusReply.from(
            buildMap {
                put("st", st)
                put("rc", rc)
                srv?.let { put("srv", it) }
                ipt?.let { put("ipt", it) }
                put("u", "user1@ting")
                put("pw", "s3cret")
            },
        )

    @Test
    fun `not the phone app stops first`() =
        assertEquals(VvmAction.Stop(VvmText.NOT_PHONE_APP), startAction(false, true, cvvm, "310260"))

    @Test
    fun `no SIM stops`() = assertEquals(VvmAction.Stop(VvmText.NO_SIM), startAction(true, false, null, null))

    @Test
    fun `no config stops with the operator`() =
        assertEquals(VvmAction.Stop(VvmText.noConfig("311480")), startAction(true, true, null, "311480"))

    @Test
    fun `ready start asks for status`() = assertEquals(VvmAction.SendStatus, startAction(true, true, cvvm, "310260"))

    @Test
    fun `ready reply logs in`() {
        val r = reply("R")
        assertEquals(VvmAction.Login(r), actionForStatus(r, false))
    }

    @Test
    fun `new or provisioned activates once`() {
        assertEquals(VvmAction.SendActivate, actionForStatus(reply("N"), false))
        assertEquals(VvmAction.SendActivate, actionForStatus(reply("P"), false))
        assertEquals(VvmAction.Stop(VvmText.STILL_NOT_READY), actionForStatus(reply("N"), true))
    }

    @Test
    fun `blocked stops`() = assertEquals(VvmAction.Stop(VvmText.BLOCKED), actionForStatus(reply("B"), false))

    @Test
    fun `ready without a server stops`() =
        assertEquals(VvmAction.Stop(VvmText.NO_SERVER), actionForStatus(reply("R", srv = null), false))

    @Test
    fun `missing imap port defaults to 143`() = assertEquals(143, reply("R", ipt = null).imapPort)

    @Test
    fun `toString never shows credentials`() {
        val s = reply("R").toString()
        assertFalse(s.contains("user1"))
        assertFalse(s.contains("s3cret"))
    }

    @Test
    fun `sync new message rechecks`() = assertEquals(VvmAction.SendStatus, actionForSync("NM"))

    @Test
    fun `sync while already waiting sends nothing more`() =
        assertEquals(VvmAction.Stop(VvmText.ALREADY_ASKED), actionForSync("NM", alreadyWaiting = true))

    @Test
    fun `a reply only counts shortly after asking`() {
        assertFalse(expectingReply(null, 1_000))
        assertTrue(expectingReply(1_000, 1_000 + REPLY_WINDOW_MILLIS))
        assertFalse(expectingReply(1_000, 1_000 + REPLY_WINDOW_MILLIS + 1))
        assertFalse(expectingReply(5_000, 1_000))
    }

    @Test
    fun `config line names the source`() =
        assertEquals("Carrier settings: CVVM, number 122, port 1808 (from the SIM).", VvmText.config(cvvm))
}
