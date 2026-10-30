package io.github.secnewt.dialer.voicemail

import java.io.BufferedReader
import java.io.Writer

/** How the login to Ting's voicemail server went. */
sealed interface ProbeResult {
    data class LoggedIn(val messages: Int) : ProbeResult
    data class Refused(val serverText: String) : ProbeResult
    data class Failed(val line: String) : ProbeResult
}

/** Thrown inside the probe when the server hangs up mid-conversation. */
private class ClosedEarly : Exception()

private val EXISTS = Regex("""^\* (\d+) EXISTS""", RegexOption.IGNORE_CASE)

/**
 * Logs in, counts the messages in INBOX and logs out, over streams the caller opened.
 * Only the server's own words come back; the user and password never appear in a result.
 */
fun probeMailbox(input: BufferedReader, output: Writer, user: String, password: String): ProbeResult {
    fun send(line: String) {
        output.write("$line\r\n")
        output.flush()
    }

    /** Reads to the tagged reply; returns its status word and text, after handing untagged lines to [untagged]. */
    fun await(tag: String, untagged: (String) -> Unit = {}): Pair<String, String> {
        while (true) {
            val line = input.readLine() ?: throw ClosedEarly()
            if (line.startsWith("$tag ")) {
                val rest = line.removePrefix("$tag ")
                val status = rest.substringBefore(' ').uppercase()
                return status to rest.substringAfter(' ', "")
            }
            untagged(line)
        }
    }

    return try {
        input.readLine() ?: throw ClosedEarly() // greeting
        send("a1 LOGIN ${quote(user)} ${quote(password)}")
        val (loginStatus, loginText) = await("a1")
        if (loginStatus != "OK") return ProbeResult.Refused(loginText)

        send("a2 SELECT INBOX")
        var count = 0
        val (selectStatus, selectText) = await("a2") { line ->
            EXISTS.find(line)?.let { count = it.groupValues[1].toInt() }
        }
        if (selectStatus != "OK") return ProbeResult.Failed(VvmText.failed("the inbox wouldn't open ($selectText)"))

        send("a3 LOGOUT")
        try {
            await("a3")
        } catch (_: ClosedEarly) {
            // Some servers hang up straight after BYE; the count is already in.
        }
        ProbeResult.LoggedIn(count)
    } catch (_: ClosedEarly) {
        ProbeResult.Failed(VvmText.failed("the server closed the connection"))
    }
}

/** IMAP quoted string: backslash-escape quotes and backslashes. */
private fun quote(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

fun probeLine(result: ProbeResult): String = when (result) {
    is ProbeResult.LoggedIn -> VvmText.loggedIn(result.messages)
    is ProbeResult.Refused -> VvmText.loginRefused(result.serverText)
    is ProbeResult.Failed -> result.line
}

/**
 * A plain line for a connection error. Only the exception's class name is shown, since socket
 * messages can carry the server and other details; GrapheneOS's Network permission gets its own line.
 */
fun connectFailureLine(e: Exception): String {
    var cause: Throwable? = e
    while (cause != null) {
        val message = cause.message.orEmpty()
        if ("EACCES" in message || "Permission denied" in message) return VvmText.NETWORK_PERMISSION
        cause = cause.cause
    }
    return VvmText.failed(e.javaClass.simpleName)
}
