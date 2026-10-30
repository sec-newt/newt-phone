package io.github.secnewt.dialer.voicemail

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** One step shown in Settings, with when it happened. */
data class StatusLine(val atMillis: Long, val text: String)

/**
 * The visual voicemail setting, Ting's login details and the recent status lines, in the app's
 * private storage (which is never backed up). The login details never leave this file.
 */
class VoicemailStore(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /** Whether the one "turn it on" text has gone out since the setting was turned on. */
    var activateSent: Boolean
        get() = prefs.getBoolean(KEY_ACTIVATE_SENT, false)
        set(value) = prefs.edit().putBoolean(KEY_ACTIVATE_SENT, value).apply()

    /** When the app last texted Ting a request, or null; replies only count soon after. */
    var requestSentAt: Long?
        get() = prefs.getLong(KEY_REQUEST_SENT_AT, -1).takeIf { it >= 0 }
        set(value) = prefs.edit().apply { if (value == null) remove(KEY_REQUEST_SENT_AT) else putLong(KEY_REQUEST_SENT_AT, value) }.apply()

    fun saveCredentials(reply: StatusReply) {
        prefs.edit()
            .putString(KEY_ST, reply.provisioning)
            .putString(KEY_RC, reply.returnCode)
            .putString(KEY_HOST, reply.host)
            .putInt(KEY_PORT, reply.imapPort)
            .putString(KEY_USER, reply.user)
            .putString(KEY_PASSWORD, reply.password)
            .apply()
    }

    fun credentials(): StatusReply? {
        val host = prefs.getString(KEY_HOST, null) ?: return null
        return StatusReply(
            provisioning = prefs.getString(KEY_ST, null).orEmpty(),
            returnCode = prefs.getString(KEY_RC, null).orEmpty(),
            host = host,
            imapPort = prefs.getInt(KEY_PORT, 143),
            user = prefs.getString(KEY_USER, null),
            password = prefs.getString(KEY_PASSWORD, null),
        )
    }

    fun clearCredentials() {
        prefs.edit().remove(KEY_ST).remove(KEY_RC).remove(KEY_HOST).remove(KEY_PORT)
            .remove(KEY_USER).remove(KEY_PASSWORD).apply()
    }

    /** Adds a line, keeping the newest [MAX_LINES]. The saved login is blanked out as a backstop. */
    fun addLine(text: String, nowMillis: Long) {
        var safe = text
        listOfNotNull(prefs.getString(KEY_USER, null), prefs.getString(KEY_PASSWORD, null))
            .filter { it.isNotEmpty() }
            .forEach { secret -> safe = safe.replace(secret, "(hidden)") }
        val lines = (lines() + StatusLine(nowMillis, safe)).takeLast(MAX_LINES)
        saveLines(lines)
    }

    fun lines(): List<StatusLine> {
        val json = prefs.getString(KEY_LINES, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            List(array.length()) { i ->
                val item = array.getJSONObject(i)
                StatusLine(item.getLong("at"), item.getString("text"))
            }
        } catch (_: org.json.JSONException) {
            emptyList()
        }
    }

    fun clearLines() = prefs.edit().remove(KEY_LINES).apply()

    /** Off: forget the login and the steps, and start over next time. */
    fun turnOff(nowMillis: Long = System.currentTimeMillis()) {
        enabled = false
        activateSent = false
        requestSentAt = null
        clearCredentials()
        saveLines(listOf(StatusLine(nowMillis, VvmText.OFF)))
    }

    private fun saveLines(lines: List<StatusLine>) {
        val array = JSONArray()
        lines.forEach { array.put(JSONObject().put("at", it.atMillis).put("text", it.text)) }
        prefs.edit().putString(KEY_LINES, array.toString()).apply()
    }

    companion object {
        const val PREFS_NAME = "voicemail"
        const val MAX_LINES = 20
        private const val KEY_ENABLED = "enabled"
        private const val KEY_ACTIVATE_SENT = "activate_sent"
        private const val KEY_REQUEST_SENT_AT = "request_sent_at"
        private const val KEY_ST = "st"
        private const val KEY_RC = "rc"
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_USER = "user"
        private const val KEY_PASSWORD = "password"
        private const val KEY_LINES = "lines"
    }
}
