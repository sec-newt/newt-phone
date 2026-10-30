package io.github.secnewt.dialer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.secnewt.dialer.voicemail.StatusLine
import io.github.secnewt.dialer.voicemail.VvmText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** How long a text to Ting can go unanswered before Settings says so. */
private const val WAIT_MILLIS = 2 * 60 * 1000L

/** "Ting hasn't answered the text yet." once the newest step is a text sent over 2 minutes ago. */
fun waitingHint(lines: List<StatusLine>, nowMillis: Long): String? {
    val newest = lines.lastOrNull() ?: return null
    val sentText = newest.text == VvmText.STATUS_SENT || newest.text == VvmText.ACTIVATE_SENT
    return if (sentText && nowMillis - newest.atMillis > WAIT_MILLIS) VvmText.WAITING else null
}

/** Visual voicemail on or off, and while it's being tried out, each step in plain words. */
@Composable
fun VoicemailSection(enabled: Boolean, lines: List<StatusLine>, nowMillis: Long, onEnabledChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SwitchRow(
            title = "Visual voicemail",
            detail = "Download your voicemails and read them here. Uses mobile data to reach Ting, nothing else.",
            checked = enabled,
            onChange = onEnabledChange,
        )
        if (enabled) {
            Text("Status", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            lines.forEach { line ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        Instant.ofEpochMilli(line.atMillis).atZone(ZoneId.systemDefault()).format(time),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(line.text, style = MaterialTheme.typography.bodyLarge)
                }
            }
            waitingHint(lines, nowMillis)?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
