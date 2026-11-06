package io.github.secnewt.dialer.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.secnewt.dialer.screening.CallAction
import io.github.secnewt.dialer.screening.ProtectionLevel
import io.github.secnewt.dialer.screening.SpamSettings
import io.github.secnewt.ui.NewtChoice
import io.github.secnewt.ui.NewtLockedRow
import io.github.secnewt.ui.NewtSettingsCard
import io.github.secnewt.ui.NewtSwitchRow
import io.github.secnewt.ui.NewtTone
import io.github.secnewt.ui.NewtValueRow
import io.github.secnewt.ui.ScreenHeader
import io.github.secnewt.ui.neonFieldColors
import io.github.secnewt.ui.newtContainerColor
import java.util.Date

/** The kinds of unknown callers a person can set an action for. */
private enum class CallerType(val title: String, val explanation: String) {
    NEW_NUMBERS(
        "New numbers",
        "Not in your contacts, and you've never called them. Choose Ring while you expect calls from new people, " +
            "like during a job search, or use Expecting a call below for a few hours. A second call within 3 " +
            "minutes always rings, and once you call a number back or add it to contacts, it rings from then on.",
    ),
    FAKE_NUMBERS(
        "Made-up numbers",
        "Numbers from an exchange no phone company has, so the caller ID was faked. Checked against the " +
            "national numbering list built into the app.",
    ),
    LIKELY_SPAM("Likely spam", "Calls your carrier could not verify."),
    HIDDEN("Hidden numbers", "Private or unknown callers. Doctors and pharmacies sometimes call this way."),
    COPYCAT("Copycat numbers", "Numbers that start like yours, a common scam trick."),
}

/** Hours for "Expecting a call"; 0 turns it off. */
private val EXPECTING_HOURS = listOf(0, 1, 3, 8)

private fun expectingName(hours: Int) = when (hours) {
    0 -> "Off"
    1 -> "For 1 hour"
    else -> "For $hours hours"
}

private fun expectingExplanation(hours: Int) =
    if (hours == 0) "New numbers follow the choice above" else "Then back to normal by itself"

/** Ring is good news, Silence is information, Block is the strongest. */
private fun actionTone(action: CallAction) = when (action) {
    CallAction.RING -> NewtTone.Good
    CallAction.SILENCE -> NewtTone.Info
    CallAction.BLOCK -> NewtTone.Danger
}

/**
 * What happens to calls from numbers that aren't contacts: a preset level, the action for each
 * kind of caller, and who always gets through.
 */
@Composable
fun UnknownCallersScreen(
    settings: SpamSettings,
    onSettingsChange: (SpamSettings) -> Unit,
    onBack: () -> Unit,
    nowMillis: Long = System.currentTimeMillis(),
) {
    var choosing by remember { mutableStateOf<CallerType?>(null) }
    var choosingExpecting by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize(), color = newtContainerColor()) {
        LazyColumn(
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ScreenHeader("Unknown callers", onBack) }
            item {
                NewtChoice(
                    options = listOf(ProtectionLevel.OFF, ProtectionLevel.BALANCED, ProtectionLevel.STRICT),
                    selected = settings.level.takeIf { it != ProtectionLevel.CUSTOM },
                    label = ::levelName,
                    onSelect = { onSettingsChange(settings.withLevel(it)) },
                    explanation = levelDescription(settings.level),
                )
            }
            item {
                NewtSettingsCard("Numbers you don't know", icon = Icons.Filled.Warning) {
                    CallerType.entries.forEach { type ->
                        val action = settings.actionFor(type)
                        NewtValueRow(type.title, actionLabel(action), onClick = { choosing = type }, tone = actionTone(action))
                    }
                }
            }
            item {
                NewtSettingsCard("Copycat check") {
                    Column(
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            colors = neonFieldColors(),
                            value = settings.myNumber.orEmpty(),
                            onValueChange = { onSettingsChange(settings.copy(myNumber = it.ifBlank { null })) },
                            label = { Text("My phone number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            textStyle = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "Spots numbers that copy yours. It stays on this phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                NewtSettingsCard("Always let through", icon = Icons.Filled.Check) {
                    NewtSwitchRow(
                        "Call-backs",
                        settings.ringAfterICall,
                        { onSettingsChange(settings.copy(ringAfterICall = it)) },
                        hint = "1 hour after you call a new number",
                    )
                    NewtValueRow(
                        "Expecting a call",
                        if (settings.expecting(nowMillis)) {
                            "Until " + DateFormat.getTimeFormat(context).format(Date(settings.expectingUntil))
                        } else {
                            "Off"
                        },
                        onClick = { choosingExpecting = true },
                    )
                    NewtLockedRow("After 911", "Always", "Every call rings for an hour after you call 911.")
                }
            }
        }
    }

    if (choosingExpecting) {
        ChoiceDialog(
            title = "Expecting a call",
            description = "New numbers ring until then. It turns itself off.",
            options = EXPECTING_HOURS.map { hours -> hours to (expectingName(hours) to expectingExplanation(hours)) },
            current = if (settings.expecting(nowMillis)) -1 else 0,
            onChoose = { hours ->
                onSettingsChange(settings.copy(expectingUntil = if (hours == 0) 0L else nowMillis + hours * 3_600_000L))
                choosingExpecting = false
            },
            onDismiss = { choosingExpecting = false },
        )
    }

    choosing?.let { type ->
        ChoiceDialog(
            title = type.title,
            description = type.explanation,
            options = CallAction.entries.map { it to (actionLabel(it) to actionExplanation(it)) },
            current = settings.actionFor(type),
            onChoose = { action ->
                onSettingsChange(settings.withAction(type, action))
                choosing = null
            },
            onDismiss = { choosing = null },
        )
    }
}

private fun SpamSettings.actionFor(type: CallerType) = when (type) {
    CallerType.NEW_NUMBERS -> newNumbers
    CallerType.FAKE_NUMBERS -> fakeNumbers
    CallerType.LIKELY_SPAM -> likelySpam
    CallerType.HIDDEN -> hiddenNumbers
    CallerType.COPYCAT -> copycatNumbers
}

private fun SpamSettings.withAction(type: CallerType, action: CallAction) = when (type) {
    CallerType.NEW_NUMBERS -> withNewNumbers(action)
    CallerType.FAKE_NUMBERS -> withFakeNumbers(action)
    CallerType.LIKELY_SPAM -> withLikelySpam(action)
    CallerType.HIDDEN -> withHiddenNumbers(action)
    CallerType.COPYCAT -> withCopycatNumbers(action)
}
