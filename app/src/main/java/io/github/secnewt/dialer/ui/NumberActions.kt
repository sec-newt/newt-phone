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

/** What you can do with a number (or the contact it belongs to) from a tap-and-hold menu. */
enum class NumberAction(val label: String) {
    PASTE("Paste number"),
    COPY("Copy number"),
    ADD_CONTACT("Add to contacts"),
    EDIT_CONTACT("Edit contact"),
    MESSAGE("Send a text"),
    EDIT("Edit before calling"),
    BLOCK("Block number"),
}

/** Recents: no "Add" for a saved contact, no "Block" twice. */
fun numberActions(isContact: Boolean, isBlocked: Boolean): List<NumberAction> = listOfNotNull(
    NumberAction.COPY,
    NumberAction.ADD_CONTACT.takeUnless { isContact },
    NumberAction.MESSAGE,
    NumberAction.EDIT,
    NumberAction.BLOCK.takeUnless { isBlocked },
)

/** The number typed on the dialpad: paste one in, or copy, save or text what's there. */
fun dialpadActions(hasNumber: Boolean): List<NumberAction> =
    if (hasNumber) {
        listOf(NumberAction.PASTE, NumberAction.COPY, NumberAction.ADD_CONTACT, NumberAction.MESSAGE)
    } else {
        listOf(NumberAction.PASTE)
    }

/** A contact in Contacts or Favorites: edit it in the contacts app, or use its first number. */
fun contactActions(hasNumber: Boolean): List<NumberAction> =
    if (hasNumber) {
        listOf(NumberAction.EDIT_CONTACT, NumberAction.COPY, NumberAction.MESSAGE)
    } else {
        listOf(NumberAction.EDIT_CONTACT)
    }

/** One of a contact's numbers, on the contact's page. */
fun phoneActions(isBlocked: Boolean): List<NumberAction> = listOfNotNull(
    NumberAction.COPY,
    NumberAction.MESSAGE,
    NumberAction.EDIT,
    NumberAction.BLOCK.takeUnless { isBlocked },
)

/** A list of large buttons, opened by tapping or holding a number or contact. */
@Composable
fun NumberActionsDialog(
    title: String,
    number: String?,
    actions: List<NumberAction>,
    onAction: (NumberAction) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                val formatted = number?.let { formatCaller(it) }
                if (formatted != null && formatted != title) {
                    Text(
                        formatted,
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
