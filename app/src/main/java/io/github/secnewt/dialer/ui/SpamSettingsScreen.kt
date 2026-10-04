package io.github.secnewt.dialer.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import io.github.secnewt.dialer.screening.CallAction
import io.github.secnewt.dialer.screening.ProtectionLevel
import io.github.secnewt.dialer.screening.SpamSettings

/** The three kinds of unknown callers a person can set an action for. */
private enum class CallerType(val title: String, val explanation: String) {
    LIKELY_SPAM("Likely spam", "Calls your carrier could not verify."),
    HIDDEN("Hidden numbers", "Private or unknown callers. Doctors and pharmacies sometimes call this way."),
    COPYCAT("Copycat numbers", "Numbers that start like yours, a common scam trick."),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpamSettingsScreen(
    settings: SpamSettings,
    blockListSize: Int,
    onSettingsChange: (SpamSettings) -> Unit,
    onOpenBlockList: () -> Unit,
    onBack: () -> Unit,
) {
    var choosing by remember { mutableStateOf<CallerType?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { ScreenHeader("Spam protection", onBack) }

            item { ObserveOnlyCard(settings.observeOnly) { onSettingsChange(settings.copy(observeOnly = it)) } }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Protection level")
                    val levels = listOf(ProtectionLevel.OFF, ProtectionLevel.BALANCED, ProtectionLevel.STRICT)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        levels.forEachIndexed { index, level ->
                            SegmentedButton(
                                selected = settings.level == level,
                                onClick = { onSettingsChange(settings.withLevel(level)) },
                                shape = SegmentedButtonDefaults.itemShape(index, levels.size),
                                modifier = Modifier.heightIn(min = 52.dp),
                            ) {
                                Text(levelName(level), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                    Text(
                        text = levelDescription(settings.level),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            item {
                Column {
                    SectionLabel("Unknown callers", modifier = Modifier.padding(bottom = 4.dp))
                    CallerTypeRow(CallerType.LIKELY_SPAM, settings.likelySpam) { choosing = it }
                    HorizontalDivider()
                    CallerTypeRow(CallerType.HIDDEN, settings.hiddenNumbers) { choosing = it }
                    HorizontalDivider()
                    CallerTypeRow(CallerType.COPYCAT, settings.copycatNumbers) { choosing = it }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = settings.myNumber.orEmpty(),
                        onValueChange = { onSettingsChange(settings.copy(myNumber = it.ifBlank { null })) },
                        label = { Text("My phone number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Used only to spot copycat numbers. It stays on this phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                NavigationRow(
                    title = "My block list",
                    value = if (blockListSize == 1) "1 entry" else "$blockListSize entries",
                    onClick = onOpenBlockList,
                )
            }
        }
    }

    choosing?.let { type ->
        val current = when (type) {
            CallerType.LIKELY_SPAM -> settings.likelySpam
            CallerType.HIDDEN -> settings.hiddenNumbers
            CallerType.COPYCAT -> settings.copycatNumbers
        }
        ActionChoiceDialog(
            title = type.title,
            current = current,
            onChoose = { action ->
                onSettingsChange(
                    when (type) {
                        CallerType.LIKELY_SPAM -> settings.withLikelySpam(action)
                        CallerType.HIDDEN -> settings.withHiddenNumbers(action)
                        CallerType.COPYCAT -> settings.withCopycatNumbers(action)
                    }
                )
                choosing = null
            },
            onDismiss = { choosing = null },
        )
    }
}

@Composable
private fun ObserveOnlyCard(observeOnly: Boolean, onChange: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .toggleable(value = observeOnly, role = Role.Switch, onValueChange = onChange)
                .padding(20.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Observe only", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (observeOnly) {
                        "On: every call rings. Recent calls show what would have been silenced or blocked."
                    } else {
                        "Off: the rules below are applied to real calls."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // The whole row is the switch, so this one only shows the state.
            Switch(checked = observeOnly, onCheckedChange = null, modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
private fun CallerTypeRow(type: CallerType, action: CallAction, onClick: (CallerType) -> Unit) {
    NavigationRow(title = type.title, value = actionLabel(action), onClick = { onClick(type) })
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
private fun ActionChoiceDialog(
    title: String,
    current: CallAction,
    onChoose: (CallAction) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                Text("What should happen?", style = MaterialTheme.typography.bodyLarge)
                CallAction.entries.forEach { action ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .selectable(
                                selected = action == current,
                                role = Role.RadioButton,
                                onClick = { onChoose(action) },
                            ),
                    ) {
                        RadioButton(selected = action == current, onClick = null)
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(actionLabel(action), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = actionExplanation(action),
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

private fun levelName(level: ProtectionLevel) = when (level) {
    ProtectionLevel.OFF -> "Off"
    ProtectionLevel.BALANCED -> "Balanced"
    ProtectionLevel.STRICT -> "Strict"
    ProtectionLevel.CUSTOM -> "Custom"
}

private fun levelDescription(level: ProtectionLevel) = when (level) {
    ProtectionLevel.OFF -> "Only your block list is used. Everything else rings."
    ProtectionLevel.BALANCED -> "Silences likely spam, hidden and copycat numbers. Contacts always ring."
    ProtectionLevel.STRICT -> "Blocks likely spam and copycat numbers, silences hidden numbers. Contacts always ring."
    ProtectionLevel.CUSTOM -> "Custom: you changed the choices below. Contacts always ring."
}

private fun actionExplanation(action: CallAction) = when (action) {
    CallAction.RING -> "Ring like any other call"
    CallAction.SILENCE -> "No ring, still shown in recent calls"
    CallAction.BLOCK -> "Hang up, kept in recent calls for review"
}
