package io.github.secnewt.dialer.screening

import android.content.Context
import android.content.SharedPreferences
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
        const val TYPE_NUMBER = "number"
        const val TYPE_STARTS_WITH = "starts_with"
    }
}
