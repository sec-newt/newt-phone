package io.github.secnewt.dialer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.secnewt.dialer.ui.theme.DialerFonts

/** What you can do with a number from Recents, besides calling it. */
enum class NumberAction(val label: String) {
    COPY("Copy number"),
    ADD_CONTACT("Add to contacts"),
    MESSAGE("Send a text"),
    EDIT("Edit before calling"),
    BLOCK("Block number"),
}

/** The actions that make sense for a number: no "Add" for a saved contact, no "Block" twice. */
fun numberActions(isContact: Boolean, isBlocked: Boolean): List<NumberAction> =
    NumberAction.entries.filter {
        when (it) {
            NumberAction.ADD_CONTACT -> !isContact
            NumberAction.BLOCK -> !isBlocked
            else -> true
        }
    }

/** A list of large buttons for one number, opened by tapping or holding a Recents row. */
@Composable
fun NumberActionsDialog(
    title: String,
    number: String,
    actions: List<NumberAction>,
    onAction: (NumberAction) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (title != formatCaller(number)) {
                    Text(
                        formatCaller(number),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = DialerFonts.Mono,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            Column {
                actions.forEachIndexed { index, action ->
                    if (index > 0) HorizontalDivider()
                    TextButton(
                        onClick = { onAction(action) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    ) {
                        Text(
                            action.label,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (action == NumberAction.BLOCK) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", style = MaterialTheme.typography.titleMedium) }
        },
    )
}
