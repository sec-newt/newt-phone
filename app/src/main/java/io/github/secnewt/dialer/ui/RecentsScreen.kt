package io.github.secnewt.dialer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.secnewt.dialer.announce.AnnounceMode
import io.github.secnewt.dialer.calls.CallGroup
import io.github.secnewt.dialer.calls.CallHistory
import io.github.secnewt.dialer.calls.CallKind
import io.github.secnewt.dialer.contacts.Contact
import io.github.secnewt.dialer.screening.CallAction
import io.github.secnewt.dialer.screening.PhoneNumbers
import io.github.secnewt.dialer.screening.Reason
import io.github.secnewt.dialer.screening.ScreenedCall
import io.github.secnewt.dialer.screening.Verification
import io.github.secnewt.dialer.ui.theme.DialerFonts
import java.time.ZonedDateTime
import kotlin.math.abs

/** The phone's call history, with spam labels from the screening log and one-tap call back. */
@Composable
fun RecentsScreen(
    groups: List<CallGroup>,
    hasAccess: Boolean,
    screened: List<ScreenedCall>,
    roleHeld: Boolean,
    observeOnly: Boolean,
    announceMode: AnnounceMode,
    onOpenSettings: () -> Unit,
    onOpenSpamProtection: () -> Unit,
    onEnableSpamProtection: () -> Unit,
    onAllowAccess: () -> Unit,
    onCall: (String) -> Unit,
    now: ZonedDateTime = ZonedDateTime.now(),
    message: String? = null,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TabHeader("Recents", onOpenSettings) }
            message?.let {
                item { Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold) }
            }
            item {
                StatusPanel(
                    roleHeld = roleHeld,
                    observeOnly = observeOnly,
                    announceMode = announceMode,
                    onOpenSpamProtection = onOpenSpamProtection,
                    onOpenSettings = onOpenSettings,
                    onEnable = onEnableSpamProtection,
                )
            }
            if (!hasAccess) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            "To show your recent calls, the app needs access to your call history. " +
                                "It stays on this phone.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Button(
                            onClick = onAllowAccess,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                        ) {
                            Text("Allow call history access", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                return@LazyColumn
            }
            if (groups.isEmpty()) {
                item {
                    Text(
                        "No calls yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(groups, key = { it.latest.id }) { group ->
                    RecentRow(group, spamLabel(group, screened), now, onCall)
                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/** "Likely spam" or "Blocked number" when the screening log flagged this call. */
private fun spamLabel(group: CallGroup, screened: List<ScreenedCall>): String? {
    val call = group.latest
    val number = PhoneNumbers.normalize(call.number) ?: return null
    val match = screened.firstOrNull {
        PhoneNumbers.normalize(it.number) == number && abs(it.timeMillis - call.timeMillis) < 2 * 60_000
    } ?: return null
    return when {
        match.reason == Reason.BLOCK_LIST -> "Blocked number"
        match.action != CallAction.RING || match.verification == Verification.FAILED -> "Likely spam"
        else -> null
    }
}

@Composable
private fun StatusPanel(
    roleHeld: Boolean,
    observeOnly: Boolean,
    announceMode: AnnounceMode,
    onOpenSpamProtection: () -> Unit,
    onOpenSettings: () -> Unit,
    onEnable: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            NavigationRow(
                title = "Spam protection",
                value = when {
                    !roleHeld -> "Off"
                    observeOnly -> "Observe only"
                    else -> "On"
                },
                onClick = onOpenSpamProtection,
            )
            HorizontalDivider()
            NavigationRow(title = "Announcing callers", value = announceModeName(announceMode), onClick = onOpenSettings)
            if (!roleHeld) {
                Button(
                    onClick = onEnable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .heightIn(min = 56.dp),
                ) {
                    Text("Turn on spam protection", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun RecentRow(group: CallGroup, spam: String?, now: ZonedDateTime, onCall: (String) -> Unit) {
    val call = group.latest
    val number = call.number?.takeIf { it.isNotBlank() }
    val title = call.name ?: formatCaller(number)
    val missed = call.kind == CallKind.MISSED
    val avatar = Contact(call.id, title, starred = false, photoUri = call.photoUri, thumbnailUri = call.photoUri)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
    ) {
        ContactAvatar(
            contact = avatar,
            sizePx = with(LocalDensity.current) { 48.dp.roundToPx() },
            initialStyle = MaterialTheme.typography.titleLarge,
            shape = CircleShape,
            modifier = Modifier.size(48.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = if (call.name == null && number != null) DialerFonts.Mono else DialerFonts.Body,
                fontWeight = FontWeight.Bold,
                color = if (missed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            )
            KindLine(call.kind, CallHistory.summary(group), missed)
            Text(
                text = formatCallTime(call.timeMillis, now),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            spam?.let { SpamTag(it) }
        }
        if (number != null) {
            FilledTonalIconButton(onClick = { onCall(number) }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.Call, contentDescription = "Call $title", modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun KindLine(kind: CallKind, summary: String, missed: Boolean) {
    val (icon, angle) = kindIcon(kind)
    val color = if (missed || kind == CallKind.BLOCKED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(20.dp)
                .rotate(angle),
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (missed) FontWeight.Bold else FontWeight.Normal,
            color = color,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** Arrow pointing out for calls you made, in for calls you got; an X for blocked or declined. */
private fun kindIcon(kind: CallKind): Pair<ImageVector, Float> = when (kind) {
    CallKind.OUTGOING -> Icons.AutoMirrored.Filled.ArrowForward to -45f
    CallKind.INCOMING, CallKind.MISSED -> Icons.AutoMirrored.Filled.ArrowForward to 135f
    CallKind.DECLINED, CallKind.BLOCKED -> Icons.Filled.Close to 0f
    CallKind.VOICEMAIL, CallKind.OTHER -> Icons.Filled.Info to 0f
}

@Composable
private fun SpamTag(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { contentDescription = text },
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = DialerFonts.Display,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** Tokyo Night green for the main Call button; dark icon on it stays above 7:1. */
val CallGreen = Color(0xFF9ECE6A)
val OnCallGreen = Color(0xFF0B0C10)
