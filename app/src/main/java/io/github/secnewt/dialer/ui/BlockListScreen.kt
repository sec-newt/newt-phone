package io.github.secnewt.dialer.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.secnewt.dialer.screening.BlockRule
import io.github.secnewt.dialer.screening.PhoneNumbers

@Composable
fun BlockListScreen(
    rules: List<BlockRule>,
    onAdd: (BlockRule) -> Unit,
    onRemove: (BlockRule) -> Unit,
    onBack: () -> Unit,
    sharedWithAndroid: Boolean = false,
) {
    var adding by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ScreenHeader("Block list", onBack) }
            item {
                Text(
                    text = "Calls from these numbers are always blocked, even when protection is Off.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (sharedWithAndroid) {
                item {
                    Text(
                        text = "Exact numbers are also on Android's own block list, so they're turned away " +
                            "before ringing and their texts are blocked too. \"Starting with\" rules stay in " +
                            "this app.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Button(
                    onClick = { adding = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text("Add a number", style = MaterialTheme.typography.titleMedium)
                }
            }
            if (rules.isEmpty()) {
                item {
                    Text(
                        text = "Nothing blocked yet. You can also block a number from Recent calls.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(rules) { rule ->
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp),
                        ) {
                            Text(
                                text = ruleLabel(rule),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = { onRemove(rule) },
                                modifier = Modifier.semantics { contentDescription = "Remove ${ruleLabel(rule)}" },
                            ) {
                                Text("Remove", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (adding) {
        AddBlockRuleDialog(
            onAdd = {
                onAdd(it)
                adding = false
            },
            onDismiss = { adding = false },
        )
    }
}

@Composable
private fun AddBlockRuleDialog(onAdd: (BlockRule) -> Unit, onDismiss: () -> Unit) {
    var startsWith by remember { mutableStateOf(false) }
    var digits by remember { mutableStateOf("") }
    val valid = PhoneNumbers.normalize(digits) != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Block a number") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.selectableGroup()) {
                    ChoiceRow("This number", selected = !startsWith) { startsWith = false }
                    ChoiceRow("Numbers starting with…", selected = startsWith) { startsWith = true }
                }
                OutlinedTextField(
                    colors = neonFieldColors(),
                    value = digits,
                    onValueChange = { digits = it },
                    label = { Text(if (startsWith) "First digits" else "Phone number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onAdd(if (startsWith) BlockRule.StartsWith(digits.trim()) else BlockRule.Number(digits.trim())) },
            ) { Text("Block") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp))
    }
}
