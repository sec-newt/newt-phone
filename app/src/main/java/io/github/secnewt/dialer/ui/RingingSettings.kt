package io.github.secnewt.dialer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import io.github.secnewt.ui.NewtSwitchRow
import io.github.secnewt.ui.NewtValueRow

/** "10:00 PM" for 22 * 60, in the phone's own clock style. */
fun quietTimeLabel(minutesAfterMidnight: Int): String =
    LocalTime.of(minutesAfterMidnight / 60, minutesAfterMidnight % 60)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

/** Who rings through silent mode, and quiet hours. */
@Composable
fun RingingSection(settings: RingSettings, isPhoneApp: Boolean?, onChange: (RingSettings) -> Unit) {
    var picking by remember { mutableStateOf<QuietEnd?>(null) }
    NewtSwitchRow(
        title = "Favorites",
        checked = settings.starredRingThrough,
        onCheckedChange = { onChange(settings.copy(starredRingThrough = it)) },
        hint = "At alarm volume",
    )
    NewtSwitchRow(
        title = "Repeat callers",
        checked = settings.repeatRingThrough,
        onCheckedChange = { onChange(settings.copy(repeatRingThrough = it)) },
        hint = "Twice in 3 minutes, never likely spam",
    )
    NewtSwitchRow(
        title = "Quiet hours",
        checked = settings.quietHours.enabled,
        onCheckedChange = { onChange(settings.copy(quietHours = settings.quietHours.copy(enabled = it))) },
        hint = "Only those two ring, at night",
    )
    if (settings.quietHours.enabled) {
        NewtValueRow("Starts", quietTimeLabel(settings.quietHours.startMinutes), onClick = { picking = QuietEnd.START })
        NewtValueRow("Ends", quietTimeLabel(settings.quietHours.endMinutes), onClick = { picking = QuietEnd.END })
    }
    if (isPhoneApp == false) {
        Text(
            text = "These work once Newt Phone is your phone app.",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(14.dp),
        )
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
