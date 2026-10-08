package io.github.secnewt.dialer.screening

import android.content.Context
import android.content.SharedPreferences
import io.github.secnewt.dialer.announce.AnnounceMode
import io.github.secnewt.dialer.announce.AnnounceSettings
import io.github.secnewt.dialer.calls.QuietHours
import io.github.secnewt.dialer.calls.RingSettings
import org.json.JSONArray
import org.json.JSONObject

/** Saves the spam settings and block list in the app's private storage. */
class SpamSettingsStore(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    fun settings(): SpamSettings {
        val defaults = SpamSettings()
        return SpamSettings(
            level = enumOr(prefs.getString(KEY_LEVEL, null), defaults.level),
            likelySpam = enumOr(prefs.getString(KEY_LIKELY_SPAM, null), defaults.likelySpam),
            hiddenNumbers = enumOr(prefs.getString(KEY_HIDDEN, null), defaults.hiddenNumbers),
            copycatNumbers = enumOr(prefs.getString(KEY_COPYCAT, null), defaults.copycatNumbers),
            observeOnly = prefs.getBoolean(KEY_OBSERVE_ONLY, defaults.observeOnly),
            myNumber = prefs.getString(KEY_MY_NUMBER, null),
        )
    }

    fun saveSettings(settings: SpamSettings) {
        prefs.edit()
            .putString(KEY_LEVEL, settings.level.name)
            .putString(KEY_LIKELY_SPAM, settings.likelySpam.name)
            .putString(KEY_HIDDEN, settings.hiddenNumbers.name)
            .putString(KEY_COPYCAT, settings.copycatNumbers.name)
            .putBoolean(KEY_OBSERVE_ONLY, settings.observeOnly)
            .putString(KEY_MY_NUMBER, settings.myNumber)
            .apply()
    }

    fun blockRules(): List<BlockRule> {
        val json = prefs.getString(KEY_BLOCK_RULES, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            List(array.length()) { i ->
                val item = array.getJSONObject(i)
                val value = item.getString("value")
                when (item.getString("type")) {
                    TYPE_STARTS_WITH -> BlockRule.StartsWith(value)
                    else -> BlockRule.Number(value)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveBlockRules(rules: List<BlockRule>) {
        val array = JSONArray()
        rules.forEach { rule ->
            array.put(
                when (rule) {
                    is BlockRule.Number -> JSONObject().put("type", TYPE_NUMBER).put("value", rule.number)
                    is BlockRule.StartsWith -> JSONObject().put("type", TYPE_STARTS_WITH).put("value", rule.digits)
                }
            )
        }
        prefs.edit().putString(KEY_BLOCK_RULES, array.toString()).apply()
    }

    fun ringSettings(): RingSettings {
        val defaults = RingSettings()
        return RingSettings(
            starredRingThrough = prefs.getBoolean(KEY_STARRED_RING_THROUGH, defaults.starredRingThrough),
            repeatRingThrough = prefs.getBoolean(KEY_REPEAT_RING_THROUGH, defaults.repeatRingThrough),
            quietHours = QuietHours(
                enabled = prefs.getBoolean(KEY_QUIET_HOURS, defaults.quietHours.enabled),
                startMinutes = prefs.getInt(KEY_QUIET_START, defaults.quietHours.startMinutes),
                endMinutes = prefs.getInt(KEY_QUIET_END, defaults.quietHours.endMinutes),
            ),
        )
    }

    fun saveRingSettings(settings: RingSettings) {
        prefs.edit()
            .putBoolean(KEY_STARRED_RING_THROUGH, settings.starredRingThrough)
            .putBoolean(KEY_REPEAT_RING_THROUGH, settings.repeatRingThrough)
            .putBoolean(KEY_QUIET_HOURS, settings.quietHours.enabled)
            .putInt(KEY_QUIET_START, settings.quietHours.startMinutes)
            .putInt(KEY_QUIET_END, settings.quietHours.endMinutes)
            .apply()
    }

    /** Whether dialpad keys beep, like the stock dialer. On unless turned off. */
    fun keypadTones(): Boolean = prefs.getBoolean(KEY_KEYPAD_TONES, true)

    fun saveKeypadTones(on: Boolean) {
        prefs.edit().putBoolean(KEY_KEYPAD_TONES, on).apply()
    }

    fun announceSettings(): AnnounceSettings {
        val defaults = AnnounceSettings()
        return AnnounceSettings(
            mode = enumOr(prefs.getString(KEY_ANNOUNCE_MODE, null), defaults.mode),
            quietDuringDnd = prefs.getBoolean(KEY_QUIET_DURING_DND, defaults.quietDuringDnd),
            repeat = prefs.getBoolean(KEY_ANNOUNCE_REPEAT, defaults.repeat),
        )
    }

    fun saveAnnounceSettings(settings: AnnounceSettings) {
        prefs.edit()
            .putString(KEY_ANNOUNCE_MODE, settings.mode.name)
            .putBoolean(KEY_QUIET_DURING_DND, settings.quietDuringDnd)
            .putBoolean(KEY_ANNOUNCE_REPEAT, settings.repeat)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private companion object {
        const val PREFS_NAME = "spam_settings"
        const val KEY_LEVEL = "level"
        const val KEY_LIKELY_SPAM = "likely_spam"
        const val KEY_HIDDEN = "hidden_numbers"
        const val KEY_COPYCAT = "copycat_numbers"
        const val KEY_OBSERVE_ONLY = "observe_only"
        const val KEY_MY_NUMBER = "my_number"
        const val KEY_BLOCK_RULES = "block_rules"
        const val KEY_ANNOUNCE_MODE = "announce_mode"
        const val KEY_QUIET_DURING_DND = "quiet_during_dnd"
        const val KEY_ANNOUNCE_REPEAT = "announce_repeat"
        const val KEY_KEYPAD_TONES = "keypad_tones"
        const val KEY_STARRED_RING_THROUGH = "starred_ring_through"
        const val KEY_REPEAT_RING_THROUGH = "repeat_ring_through"
        const val KEY_QUIET_HOURS = "quiet_hours"
        const val KEY_QUIET_START = "quiet_hours_start"
        const val KEY_QUIET_END = "quiet_hours_end"
        const val TYPE_NUMBER = "number"
        const val TYPE_STARTS_WITH = "starts_with"
    }
}
