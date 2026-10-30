package io.github.secnewt.dialer.voicemail

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The internet permission is for visual voicemail only. This fails the build if any code outside
 * the voicemail package starts using the network, so it can't quietly spread to other features.
 */
class NetworkUseTest {

    private val sourceRoot = File("src/main/java")
    private val voicemailDir = "io/github/secnewt/dialer/voicemail/"
    private val networkWords = listOf(
        "java.net.",
        "javax.net.",
        "HttpURLConnection",
        "socketFactory",
        "openConnection",
        "requestNetwork(",
        "ConnectivityManager.requestNetwork",
        // SEND_SMS is there for the voicemail texts to Ting only.
        "SmsManager",
        "sendVisualVoicemailSms",
    )

    @Test
    fun `only the voicemail package uses the network`() {
        val files = sourceRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        // A wrong path would find nothing and pass silently.
        assertTrue("Found only ${files.size} source files under ${sourceRoot.absolutePath}", files.size > 40)

        val offenders = files
            .filterNot { it.invariantSeparatorsPath.contains(voicemailDir) }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { i, line ->
                    networkWords.firstOrNull { it in line }?.let { "${file.path}:${i + 1}: $it" }
                }
            }
        assertTrue(
            "Network use outside visual voicemail (only ${voicemailDir} may go online or send texts):\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }
}
