package io.github.secnewt.dialer.ui

import java.util.Date
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.secnewt.dialer.announce.AnnounceMode
import io.github.secnewt.dialer.announce.AnnounceSettings
import io.github.secnewt.dialer.calls.RingSettings
import io.github.secnewt.dialer.screening.CallAction
import io.github.secnewt.dialer.screening.ProtectionLevel
import io.github.secnewt.dialer.screening.SpamSettings
import io.github.secnewt.ui.ScreenHeader
import io.github.secnewt.ui.NewtPrimaryButton
import io.github.secnewt.ui.NewtButton
import io.github.secnewt.ui.NewtSwitchRow
import io.github.secnewt.ui.NewtValueRow
import io.github.secnewt.ui.NewtChoice
import io.github.secnewt.ui.NewtSettingsCard
import io.github.secnewt.ui.NewtSummary
import io.github.secnewt.ui.CardTitle
import io.github.secnewt.ui.SectionLabel
import io.github.secnewt.ui.SectionHeading
import io.github.secnewt.ui.neonOutline
import io.github.secnewt.ui.neonFieldColors
import io.github.secnewt.ui.newtContainerColor

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

@Composable
fun SpamSettingsScreen(
    settings: SpamSettings,
    blockListSize: Int,
    onSettingsChange: (SpamSettings) -> Unit,
    onOpenBlockList: () -> Unit,
    onBack: () -> Unit,
    announce: AnnounceSettings = AnnounceSettings(),
    announceMessage: String? = null,
    onAnnounceModeChange: (AnnounceMode) -> Unit = {},
    onQuietDuringDndChange: (Boolean) -> Unit = {},
    onRepeatChange: (Boolean) -> Unit = {},
    onTestAnnouncement: () -> Unit = {},
    isPhoneApp: Boolean? = null,
    onMakePhoneApp: () -> Unit = {},
    phoneAppDenied: Boolean = false,
    onOpenSystemSetting: (SystemSetting) -> Unit = {},
    keypadTones: Boolean = true,
    onKeypadTonesChange: (Boolean) -> Unit = {},
    ringSettings: RingSettings = RingSettings(),
    onRingSettingsChange: (RingSettings) -> Unit = {},
    onOpenUnknownCallers: () -> Unit = {},
    roleHeld: Boolean? = null,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = newtContainerColor()) {
        LazyColumn(
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ScreenHeader("Settings", onBack) }
            item { NewtSummary("Right now", settingsSummary(settings, ringSettings, announce, roleHeld)) }

            if (isPhoneApp != null) {
                item {
                    PhoneAppCard(
                        isPhoneApp = isPhoneApp,
                        denied = phoneAppDenied,
                        onMakePhoneApp = onMakePhoneApp,
                        onOpenAppInfo = { onOpenSystemSetting(SystemSetting.APP_INFO) },
                    )
                }
            }

            item {
                NewtSettingsCard("Say who's calling", icon = Icons.Filled.Call) {
                    AnnounceSection(
                        announce = announce,
                        message = announceMessage,
                        onModeChange = onAnnounceModeChange,
                        onQuietDuringDndChange = onQuietDuringDndChange,
                        onRepeatChange = onRepeatChange,
                        onTest = onTestAnnouncement,
                        onOpenVoiceSettings = { onOpenSystemSetting(SystemSetting.TTS) },
                    )
                }
            }

            item {
                NewtSettingsCard("Ring on silent", icon = Icons.Filled.Notifications) {
                    RingingSection(ringSettings, isPhoneApp, onRingSettingsChange)
                }
            }

            item {
                NewtSettingsCard("Spam protection", icon = Icons.Filled.Warning) {
                    NewtChoice(
                        options = listOf(true, false),
                        selected = settings.observeOnly,
                        label = { if (it) "Watch" else "Block" },
                        onSelect = { onSettingsChange(settings.copy(observeOnly = it)) },
                        explanation = if (settings.observeOnly) {
                            "Every call rings. Recents shows what would have been stopped."
                        } else {
                            "The rules are applied to real calls."
                        },
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
                    )
                    NewtValueRow("Unknown callers", levelName(settings.level), onClick = onOpenUnknownCallers)
                    NewtValueRow("Block list", "$blockListSize", onClick = onOpenBlockList)
                }
            }

            item {
                NewtSettingsCard("Phone") {
                    NewtSwitchRow("Keypad sounds", keypadTones, onKeypadTonesChange, hint = "A tone for each key")
                    listOf(SystemSetting.SOUND, SystemSetting.CALLS, SystemSetting.VOICEMAIL, SystemSetting.ACCESSIBILITY)
                        .forEach { setting ->
                            NewtValueRow(setting.title, null, onClick = { onOpenSystemSetting(setting) }, hint = setting.detail)
                        }
                }
            }
        }
    }
}

/** Whether this app answers and shows calls, with a button to make it do so. */
@Composable
private fun PhoneAppCard(
    isPhoneApp: Boolean,
    denied: Boolean,
    onMakePhoneApp: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    NewtSettingsCard(if (isPhoneApp) "This is your phone app" else "Not your phone app yet", icon = Icons.Filled.Call) {
        Column(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isPhoneApp) {
                    "Calls use this app's screens. To switch back: Settings, Apps, Default apps."
                } else {
                    "Answer calls here. The stock phone app stays installed, and you can switch back any time."
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!isPhoneApp) {
                NewtPrimaryButton("Make this my phone app", onClick = onMakePhoneApp, modifier = Modifier.fillMaxWidth())
                if (denied) {
                    Text(
                        text = "If Android said the app was denied access, it's because the app was " +
                            "installed from a file. Open App info, tap the ⋮ menu in the corner, choose " +
                            "Allow restricted settings, then come back and try again.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    NewtButton("Open App info", onClick = onOpenAppInfo, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun CallerTypeRow(type: CallerType, action: CallAction, onClick: (CallerType) -> Unit) {
    NavigationRow(title = type.title, value = actionLabel(action), onClick = { onClick(type) })
}

/** Android settings screens the app links to instead of copying them. */
enum class SystemSetting(val title: String, val detail: String) {
    SOUND("Ringtone and volume", "Ringtone, ring and call volume, vibration"),
    CALLS("Calls and SIM", "Call forwarding, call waiting, caller ID, Wi-Fi calling"),
    VOICEMAIL("Voicemail", "Voicemail number and notifications"),
    ACCESSIBILITY("Hearing aids and TTY", "Hearing aid mode, TTY and real-time text"),
    TTS("Voice and speed", "The text-to-speech voice that announces callers"),
    APP_INFO("App info", "This app's permissions and restricted settings"),
}

/** A row that opens one of Android's settings screens: title, what's there, and an arrow. */
@Composable
private fun SystemSettingRow(setting: SystemSetting, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(setting.title, style = MaterialTheme.typography.titleMedium)
            Text(
                setting.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
    }
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

/**
 * Who gets through even though they're new: call-backs after I call someone new, anyone
 * while I'm expecting a call, and everyone after an emergency call (always on).
 */
@Composable
private fun LetThroughSection(
    settings: SpamSettings,
    nowMillis: Long,
    onRingAfterICallChange: (Boolean) -> Unit,
    onChooseExpecting: () -> Unit,
) {
    val context = LocalContext.current
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .toggleable(value = settings.ringAfterICall, role = Role.Switch, onValueChange = onRingAfterICallChange),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Call-backs after I call", style = MaterialTheme.typography.titleMedium)
                Text(
                    "For an hour after you call a number that isn't a contact, new numbers ring. Offices often call back from a different number.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = settings.ringAfterICall, onCheckedChange = null)
        }
        HorizontalDivider()
        NavigationRow(
            title = "Expecting a call",
            value = if (settings.expecting(nowMillis)) {
                "Until " + DateFormat.getTimeFormat(context).format(Date(settings.expectingUntil))
            } else {
                "Off"
            },
            onClick = onChooseExpecting,
        )
        HorizontalDivider()
        Text(
            "After you call 911, every call rings for an hour so they can call you back. This is always on.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
fun NavigationRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, Pair<String, String>>>,
    current: T,
    onChoose: (T) -> Unit,
    onDismiss: () -> Unit,
    description: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 12.dp))
                }
                Text("What should happen?", style = MaterialTheme.typography.bodyLarge)
                options.forEach { (option, words) ->
                    val (label, explanation) = words
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .selectable(
                                selected = option == current,
                                role = Role.RadioButton,
                                onClick = { onChoose(option) },
                            ),
                    ) {
                        RadioButton(selected = option == current, onClick = null)
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun AnnounceSection(
    announce: AnnounceSettings,
    message: String?,
    onModeChange: (AnnounceMode) -> Unit,
    onQuietDuringDndChange: (Boolean) -> Unit,
    onRepeatChange: (Boolean) -> Unit,
    onTest: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        message?.let {
            Text(text = it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
        NewtChoice(
            options = AnnounceMode.entries,
            selected = announce.mode,
            label = {
                when (it) {
                    AnnounceMode.OFF -> "Off"
                    AnnounceMode.ALWAYS -> "Always"
                    AnnounceMode.HEADPHONES_ONLY -> "Headphones"
                }
            },
            onSelect = onModeChange,
            // The permissions are explained before they're asked for, while it's still off.
            explanation = if (announce.mode == AnnounceMode.OFF) {
                "Turning this on asks for Phone, Call log and Contacts access, used only on this phone."
            } else {
                announceModeExplanation(announce.mode) + "."
            },
        )
    }
    if (announce.mode != AnnounceMode.OFF) {
        NewtSwitchRow("Quiet in Do Not Disturb", announce.quietDuringDnd, onQuietDuringDndChange)
        NewtSwitchRow("Keep repeating", announce.repeat, onRepeatChange, hint = "Until you answer")
        NewtValueRow("Voice and speed", null, onClick = onOpenVoiceSettings)
        NewtButton(
            "Test the voice",
            onClick = onTest,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
        )
    }
}

internal fun announceModeName(mode: AnnounceMode) = when (mode) {
    AnnounceMode.OFF -> "Off"
    AnnounceMode.ALWAYS -> "Always"
    AnnounceMode.HEADPHONES_ONLY -> "With headphones"
}

private fun announceModeExplanation(mode: AnnounceMode) = when (mode) {
    AnnounceMode.OFF -> "Calls ring as usual"
    AnnounceMode.ALWAYS -> "Out loud when the ringer is on, and in headphones"
    AnnounceMode.HEADPHONES_ONLY -> "Only in wired or Bluetooth headphones"
}

private fun levelName(level: ProtectionLevel) = when (level) {
    ProtectionLevel.OFF -> "Off"
    ProtectionLevel.BALANCED -> "Balanced"
    ProtectionLevel.STRICT -> "Strict"
    ProtectionLevel.CUSTOM -> "Custom"
}

private fun levelDescription(level: ProtectionLevel) = when (level) {
    ProtectionLevel.OFF -> "Only your block list is used. Everything else rings."
    ProtectionLevel.BALANCED -> "Silences new, made-up, hidden and copycat numbers and likely spam. Contacts always ring."
    ProtectionLevel.STRICT -> "Blocks made-up and copycat numbers and likely spam, silences new and hidden numbers. Contacts always ring."
    ProtectionLevel.CUSTOM -> "Custom: you changed the choices below. Contacts always ring."
}

private fun actionExplanation(action: CallAction) = when (action) {
    CallAction.RING -> "Ring like any other call"
    CallAction.SILENCE -> "No ring, still shown in recent calls"
    CallAction.BLOCK -> "Hang up, kept in recent calls for review"
}
