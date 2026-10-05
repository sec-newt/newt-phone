package io.github.secnewt.dialer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.secnewt.dialer.announce.AnnounceMode
import io.github.secnewt.dialer.screening.BlockList
import io.github.secnewt.dialer.screening.BlockRule
import io.github.secnewt.dialer.screening.CallAction
import io.github.secnewt.dialer.screening.ScreenedCall
import io.github.secnewt.dialer.screening.Verification
import java.time.ZonedDateTime

@Composable
fun SpamProtectionScreen(
    roleHeld: Boolean,
    recentCalls: List<ScreenedCall>,
    onEnable: () -> Unit,
    now: ZonedDateTime = ZonedDateTime.now(),
    observeOnly: Boolean = true,
    blockRules: List<BlockRule> = emptyList(),
    onOpenSettings: () -> Unit = {},
    onBlock: (String) -> Unit = {},
    onUnblock: (String) -> Unit = {},
    announceMode: AnnounceMode = AnnounceMode.OFF,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Calls",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { heading() },
                    )
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(56.dp)) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
            item { StatusCard(roleHeld, observeOnly) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        NavigationRow(
                            title = "Announcing callers",
                            value = announceModeName(announceMode),
                            onClick = onOpenSettings,
                        )
                    }
                }
            }
            if (!roleHeld) {
                item {
                    Button(
                        onClick = onEnable,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    ) {
                        Text("Turn on spam protection", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text("Settings and block list", style = MaterialTheme.typography.titleMedium)
                }
            }
            item {
                Text(
                    text = "Recent calls",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .semantics { heading() },
                )
            }
            if (recentCalls.isEmpty()) {
                item {
                    Text(
                        text = if (roleHeld) {
                            "No calls yet. Calls you receive will show up here."
                        } else {
                            "Calls will show up here once spam protection is on."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(recentCalls) { call ->
                    val blockedBy = call.number?.let { BlockList.match(blockRules, it) }
                    CallRow(
                        call = call,
                        now = now,
                        onBlockList = blockedBy is BlockRule.Number,
                        onBlock = onBlock,
                        onUnblock = onUnblock,
                    )
                    HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusCard(roleHeld: Boolean, observeOnly: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (roleHeld) "Spam protection is on" else "Spam protection is off",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (roleHeld && observeOnly) {
                    "Observe only: every call rings, and recent calls show what the rules would have done."
                } else if (roleHeld) {
                    "Calls from unknown numbers are checked against your rules before they ring."
                } else {
                    "Set this app as your Caller ID & spam app so it can check calls before they ring."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CallRow(
    call: ScreenedCall,
    now: ZonedDateTime,
    onBlockList: Boolean,
    onBlock: (String) -> Unit,
    onUnblock: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = formatCaller(call.number),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = formatCallTime(call.timeMillis, now),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VerificationLabel(call.verification)
        outcomeLabel(call)?.let { OutcomeLabel(it, call.action) }
        call.number?.let { number ->
            val caller = formatCaller(number)
            TextButton(
                onClick = { if (onBlockList) onUnblock(number) else onBlock(number) },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = if (onBlockList) "Unblock $caller" else "Block $caller"
                    },
            ) {
                Text(
                    text = if (onBlockList) "Unblock" else "Block this number",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun OutcomeLabel(text: String, action: CallAction) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (action == CallAction.BLOCK) Icons.Filled.Close else Icons.Filled.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun VerificationLabel(verification: Verification) {
    val (icon: ImageVector, tint: Color, label: String) = when (verification) {
        Verification.PASSED -> Triple(
            Icons.Filled.CheckCircle, MaterialTheme.colorScheme.primary, "Verified by carrier",
        )
        Verification.FAILED -> Triple(
            Icons.Filled.Warning, MaterialTheme.colorScheme.error, "Failed carrier verification",
        )
        Verification.NONE -> Triple(
            Icons.Filled.Info, MaterialTheme.colorScheme.onSurfaceVariant, "Not verified by carrier",
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        // The word carries the meaning; the icon and color only reinforce it.
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
