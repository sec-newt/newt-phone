package io.github.secnewt.dialer.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.secnewt.dialer.announce.AnnounceMode
import io.github.secnewt.dialer.calls.CallGroup
import io.github.secnewt.dialer.calls.CallHistory
import io.github.secnewt.dialer.calls.CallKind
import io.github.secnewt.dialer.contacts.Contact
import io.github.secnewt.dialer.screening.ScreenedCall
import io.github.secnewt.dialer.screening.SpamLabel
import io.github.secnewt.ui.NewtFonts
import java.time.ZonedDateTime
import io.github.secnewt.ui.TabHeader
import io.github.secnewt.ui.newtContainerColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import io.github.secnewt.ui.NewtPrimaryButton
import io.github.secnewt.ui.NewtRuleHeading
import io.github.secnewt.ui.NewtTone
import io.github.secnewt.ui.color
import io.github.secnewt.ui.newtStripe

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
    onNumberAction: (NumberAction, String) -> Unit = { _, _ -> },
    isBlocked: (String) -> Boolean = { false },
) {
    Surface(modifier = Modifier.fillMaxSize(), color = newtContainerColor()) {
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
                groups.groupBy { CallHistory.dayLabel(it.latest.timeMillis, now) }.forEach { (day, dayGroups) ->
                    item(key = "day-$day") { DayHeading(day) }
                    items(dayGroups, key = { it.latest.id }) { group ->
                        RecentRow(group, spamLabel(group, screened), now, onCall, onNumberAction, isBlocked)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeading(day: String) = NewtRuleHeading(day)

/** "Likely spam" or "Blocked number" when the screening log flagged this call. */
private fun spamLabel(group: CallGroup, screened: List<ScreenedCall>): String? =
    SpamLabel.forCall(group.latest.number, group.latest.timeMillis, screened)

/** Spam protection and caller announcements at a glance, each a chip that opens its screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusPanel(
    roleHeld: Boolean,
    observeOnly: Boolean,
    announceMode: AnnounceMode,
    onOpenSpamProtection: () -> Unit,
    onOpenSettings: () -> Unit,
    onEnable: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val spam = when {
                !roleHeld -> "Off"
                observeOnly -> "Watching"
                else -> "On"
            }
            StatusChip("Spam: $spam", onOpenSpamProtection)
            StatusChip("Announce: ${announceModeName(announceMode)}", onOpenSettings)
        }
        if (!roleHeld) {
            NewtPrimaryButton("Turn on spam protection", onClick = onEnable, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StatusChip(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(26.dp),
        )
    }
}

/** Tap or hold a row for more: copy the number, add it to contacts, text it, edit it, block it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentRow(
    group: CallGroup,
    spam: String?,
    now: ZonedDateTime,
    onCall: (String) -> Unit,
    onNumberAction: (NumberAction, String) -> Unit,
    isBlocked: (String) -> Boolean,
) {
    val call = group.latest
    val number = call.number?.takeIf { it.isNotBlank() }
    val title = call.name ?: formatCaller(number)
    val missed = call.kind == CallKind.MISSED
    val avatar = Contact(call.id, title, starred = false, photoUri = call.photoUri, thumbnailUri = call.photoUri)
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    if (menuOpen && number != null) {
        NumberActionsDialog(
            title = title,
            number = number,
            actions = numberActions(isContact = call.name != null, isBlocked = isBlocked(number)),
            onAction = { action ->
                menuOpen = false
                onNumberAction(action, number)
            },
            onDismiss = { menuOpen = false },
        )
    }
    val openMenu = if (number != null) {
        Modifier.combinedClickable(
            onClickLabel = "More options",
            onLongClickLabel = "More options",
            onLongClick = { menuOpen = true },
            onClick = { menuOpen = true },
        )
    } else {
        Modifier
    }
    val flagged = missed || spam != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .then(if (flagged) Modifier.newtStripe(MaterialTheme.colorScheme.error) else Modifier)
            .then(openMenu)
            .padding(start = if (flagged) 10.dp else 0.dp, top = 6.dp, bottom = 6.dp),
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
                fontFamily = if (call.name == null && number != null) NewtFonts.Mono else bodyFont(),
                fontWeight = FontWeight.Bold,
                color = if (missed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            )
            KindLine(
                call.kind,
                CallHistory.summary(group) + " · " + CallHistory.clock(call.timeMillis, now.zone),
                missed,
            )
            spam?.let { SpamTag(it) }
        }
        if (number != null) {
            val green = NewtTone.Good.color()
            val shape = RoundedCornerShape(18.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(58.dp)
                    .clip(shape)
                    .border(2.5.dp, green, shape)
                    .clickable(role = Role.Button) { onCall(number) },
            ) {
                Icon(Icons.Filled.Call, contentDescription = "Call $title", tint = green, modifier = Modifier.size(28.dp))
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
            style = MaterialTheme.typography.bodyMedium,
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** Tokyo Night green for the main Call button; dark icon on it stays above 7:1. */
val CallGreen = Color(0xFF9ECE6A)
val OnCallGreen = Color(0xFF0B0C10)
