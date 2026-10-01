package io.github.secnewt.dialer.screening

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Whether the carrier vouched for the caller ID (STIR/SHAKEN). */
enum class Verification { PASSED, FAILED, NONE }

/** What the app did with the call. Only ALLOWED for now; blocking comes later. */
enum class Decision { ALLOWED }

data class ScreenedCall(
    val timeMillis: Long,
    /** The caller's number, or null when it was hidden. */
    val number: String?,
    val verification: Verification,
    val decision: Decision,
)

/**
 * The list of recently screened calls, newest first, kept in a small file in
 * the app's private storage. It never leaves the phone.
 */
class ScreeningLog(private val file: File, private val maxEntries: Int = MAX_ENTRIES) {

    constructor(context: Context) : this(File(context.filesDir, FILE_NAME))

    fun add(call: ScreenedCall) = synchronized(lock) {
        write((listOf(call) + readUnlocked()).take(maxEntries))
    }

    fun read(): List<ScreenedCall> = synchronized(lock) { readUnlocked() }

    private fun readUnlocked(): List<ScreenedCall> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            List(array.length()) { i -> array.getJSONObject(i).toCall() }
        } catch (e: Exception) {
            // A damaged log is not worth crashing over; start a fresh one.
            emptyList()
        }
    }

    private fun write(calls: List<ScreenedCall>) {
        val array = JSONArray()
        calls.forEach { array.put(it.toJson()) }
        // Write to a temporary file first so a crash never leaves a half-written log.
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(array.toString())
        temp.renameTo(file)
    }

    private fun ScreenedCall.toJson() = JSONObject().apply {
        put("time", timeMillis)
        put("number", number ?: JSONObject.NULL)
        put("verification", verification.name)
        put("decision", decision.name)
    }

    private fun JSONObject.toCall() = ScreenedCall(
        timeMillis = getLong("time"),
        number = if (isNull("number")) null else getString("number"),
        verification = Verification.valueOf(getString("verification")),
        decision = Decision.valueOf(getString("decision")),
    )

    companion object {
        const val FILE_NAME = "screening-log.json"
        const val MAX_ENTRIES = 100

        // The screening service and the screen both use the log; one lock keeps them in step.
        private val lock = Any()
    }
}
