package io.github.secnewt.dialer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.secnewt.dialer.calls.RingSettings
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** "10:00 PM" for 22 * 60, in the phone's own clock style. */
fun quietTimeLabel(minutesAfterMidnight: Int): String =
    LocalTime.of(minutesAfterMidnight / 60, minutesAfterMidnight % 60)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

/** Who rings through silent mode, and quiet hours. */
@Composable
fun RingingSection(settings: RingSettings, isPhoneApp: Boolean?, onChange: (RingSettings) -> Unit) {
    var picking by remember { mutableStateOf<QuietEnd?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Who still rings when the phone is on silent or vibrate, and a quiet time each night.",
            style = MaterialTheme.typography.bodyLarge,
        )
        SwitchRow(
            title = "Starred contacts ring on silent",
            detail = "Your favorites ring at alarm volume.",
            checked = settings.starredRingThrough,
            onChange = { onChange(settings.copy(starredRingThrough = it)) },
        )
        HorizontalDivider()
        SwitchRow(
            title = "Repeat callers ring on silent",
            detail = "Anyone who calls again within 3 minutes. Never likely spam.",
            checked = settings.repeatRingThrough,
            onChange = { onChange(settings.copy(repeatRingThrough = it)) },
        )
        HorizontalDivider()
        SwitchRow(
            title = "Quiet hours",
            detail = "Only starred contacts and repeat callers ring. Other calls still show, silently.",
            checked = settings.quietHours.enabled,
            onChange = { onChange(settings.copy(quietHours = settings.quietHours.copy(enabled = it))) },
        )
        if (settings.quietHours.enabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { picking = QuietEnd.START },
                    border = neonOutline(),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                ) {
                    Text("From ${quietTimeLabel(settings.quietHours.startMinutes)}", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(
                    onClick = { picking = QuietEnd.END },
                    border = neonOutline(),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                ) {
                    Text("To ${quietTimeLabel(settings.quietHours.endMinutes)}", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        if (isPhoneApp == false) {
            Text(
                text = "These work once Newt Phone is your phone app.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    picking?.let { end ->
        val hours = settings.quietHours
        TimeDialog(
            title = if (end == QuietEnd.START) "Quiet hours start" else "Quiet hours end",
            minutes = if (end == QuietEnd.START) hours.startMinutes else hours.endMinutes,
            onPick = { minutes ->
                val updated = if (end == QuietEnd.START) hours.copy(startMinutes = minutes) else hours.copy(endMinutes = minutes)
                onChange(settings.copy(quietHours = updated))
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

private enum class QuietEnd { START, END }

@Composable
private fun SwitchRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(title: String, minutes: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) {
                Text("Set", style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", style = MaterialTheme.typography.titleMedium) }
        },
    )
}
