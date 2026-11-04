package io.github.secnewt.dialer.ui

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
import io.github.secnewt.ui.newtContainerColor

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

/** Android settings screens the app links to instead of copying them. */
enum class SystemSetting(val title: String, val detail: String) {
    SOUND("Ringtone and volume", "Ringtone, ring and call volume, vibration"),
    CALLS("Calls and SIM", "Call forwarding, call waiting, caller ID, Wi-Fi calling"),
    VOICEMAIL("Voicemail", "Voicemail number and notifications"),
    ACCESSIBILITY("Hearing aids and TTY", "Hearing aid mode, TTY and real-time text"),
    TTS("Voice and speed", "The text-to-speech voice that announces callers"),
    APP_INFO("App info", "This app's permissions and restricted settings"),
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
internal fun <T> ChoiceDialog(
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

internal fun levelName(level: ProtectionLevel) = when (level) {
    ProtectionLevel.OFF -> "Off"
    ProtectionLevel.BALANCED -> "Balanced"
    ProtectionLevel.STRICT -> "Strict"
    ProtectionLevel.CUSTOM -> "Custom"
}

internal fun levelDescription(level: ProtectionLevel) = when (level) {
    ProtectionLevel.OFF -> "Only your block list is used. Everything else rings."
    ProtectionLevel.BALANCED -> "Silences new, made-up, hidden and copycat numbers and likely spam. Contacts always ring."
    ProtectionLevel.STRICT -> "Blocks made-up and copycat numbers and likely spam, silences new and hidden numbers. Contacts always ring."
    ProtectionLevel.CUSTOM -> "Custom: you changed the rules below by hand."
}

internal fun actionExplanation(action: CallAction) = when (action) {
    CallAction.RING -> "Ring like any other call"
    CallAction.SILENCE -> "No ring, still shown in recent calls"
    CallAction.BLOCK -> "Hang up, kept in recent calls for review"
}
