package io.github.secnewt.dialer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.secnewt.dialer.announce.AnnounceMode
import io.github.secnewt.dialer.screening.BlockList
import io.github.secnewt.dialer.screening.BlockRule
import io.github.secnewt.dialer.screening.CallAction
import io.github.secnewt.dialer.screening.Reason
import io.github.secnewt.dialer.screening.ScreenedCall
import io.github.secnewt.dialer.screening.Verification
import io.github.secnewt.ui.NewtButton
import io.github.secnewt.ui.NewtFonts
import io.github.secnewt.ui.NewtPrimaryButton
import io.github.secnewt.ui.NewtSettingsCard
import io.github.secnewt.ui.NewtStateChip
import io.github.secnewt.ui.NewtSummary
import io.github.secnewt.ui.NewtTone
import io.github.secnewt.ui.NewtValueRow
import io.github.secnewt.ui.ScreenHeader
import io.github.secnewt.ui.TabHeader
import io.github.secnewt.ui.color
import io.github.secnewt.ui.newtContainerColor
import io.github.secnewt.ui.newtStripe
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

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
    /** Set when opened from Recents: shows a back button instead of the tab title. */
    onBack: (() -> Unit)? = null,
) {
    val days = recentCalls.groupBy { Instant.ofEpochMilli(it.timeMillis).atZone(now.zone).toLocalDate() }
    Surface(modifier = Modifier.fillMaxSize(), color = newtContainerColor()) {
        LazyColumn(
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                if (onBack != null) ScreenHeader("Spam protection", onBack) else TabHeader("Calls", onOpenSettings)
            }
            item { Status(roleHeld, observeOnly) }
            if (!roleHeld) {
                item {
                    NewtPrimaryButton("Turn on spam protection", onClick = onEnable, modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                NewtSettingsCard("Spam protection", icon = Icons.Filled.Settings) {
                    NewtValueRow("Announcing callers", value = announceModeName(announceMode), onClick = onOpenSettings)
                    NewtValueRow("Rules and block list", value = null, onClick = onOpenSettings)
                }
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
                days.forEach { (day, calls) ->
                    item(key = day.toString()) {
                        NewtSettingsCard(dayTitle(day, now), icon = Icons.AutoMirrored.Filled.List) {
                            calls.forEachIndexed { index, call ->
                                if (index > 0) HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                val blockedBy = call.number?.let { BlockList.match(blockRules, it) }
                                CallRow(call, now, onBlockList = blockedBy is BlockRule.Number, onBlock = onBlock, onUnblock = onUnblock)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "Today", "Yesterday", or "Tuesday, Oct 6". */
private fun dayTitle(day: LocalDate, now: ZonedDateTime): String = when (ChronoUnit.DAYS.between(day, now.toLocalDate())) {
    0L -> "Today"
    1L -> "Yesterday"
    else -> day.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
}

@Composable
private fun Status(roleHeld: Boolean, observeOnly: Boolean) {
    when {
        !roleHeld -> NewtSummary(
            "Spam protection · Off",
            "Every call rings. Turn it on to check calls before they ring.",
            tone = NewtTone.Danger,
        )
        observeOnly -> NewtSummary(
            "Spam protection · Watching",
            "Every call rings. Below is what would have been silenced.",
            tone = NewtTone.Info,
        )
        else -> NewtSummary(
            "Spam protection · On",
            "Calls from unknown numbers are checked against your rules before they ring.",
        )
    }
}

/** What happened, as a chip: "Would silence", "Silenced", "Blocked", "Verified". */
private data class Verdict(val word: String, val tone: NewtTone)

private fun verdictOf(call: ScreenedCall): Verdict? = when (call.action) {
    CallAction.SILENCE -> Verdict(if (call.enforced) "Silenced" else "Would silence", NewtTone.Danger)
    CallAction.BLOCK -> Verdict(if (call.enforced) "Blocked" else "Would block", NewtTone.Danger)
    CallAction.RING -> if (call.verification == Verification.PASSED) Verdict("Verified", NewtTone.Good) else null
}

/** Why, in a few words: "Failed carrier check", "Hidden number". */
private fun reasonOf(call: ScreenedCall): String? = when (call.reason) {
    Reason.BLOCK_LIST -> "On your block list"
    Reason.HIDDEN_NUMBER -> "Hidden number"
    Reason.COPYCAT -> "Copies your number"
    Reason.FAILED_VERIFICATION -> "Failed carrier check"
    Reason.NEW_NUMBER -> "New number"
    Reason.FAKE_NUMBER -> "Made-up number"
    Reason.NONE -> if (call.verification == Verification.FAILED) "Failed carrier check" else null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CallRow(
    call: ScreenedCall,
    now: ZonedDateTime,
    onBlockList: Boolean,
    onBlock: (String) -> Unit,
    onUnblock: (String) -> Unit,
) {
    val verdict = verdictOf(call)
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (verdict != null) Modifier.newtStripe(verdict.tone.color()) else Modifier)
            .padding(start = 20.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
    ) {
        Text(
            text = if (call.number.isNullOrBlank()) "Hidden number" else formatCaller(call.number),
            style = MaterialTheme.typography.titleLarge,
            fontFamily = if (call.number.isNullOrBlank()) bodyFont() else NewtFonts.Mono,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = formatCallTime(call.timeMillis, now),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            verdict?.let { NewtStateChip(it.word, it.tone) }
            if (onBlockList && call.action != CallAction.BLOCK) NewtStateChip("On block list", NewtTone.Danger)
            if (onBlockList && call.action == CallAction.BLOCK) NewtStateChip("On block list", NewtTone.Neutral)
        }
        reasonOf(call)?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        call.number?.let { number ->
            val caller = formatCaller(number)
            if (onBlockList) {
                NewtButton(
                    "Unblock",
                    onClick = { onUnblock(number) },
                    modifier = Modifier.semantics { contentDescription = "Unblock $caller" },
                )
            } else {
                NewtPrimaryButton(
                    "Block",
                    onClick = { onBlock(number) },
                    icon = Icons.Filled.Close,
                    tone = NewtTone.Danger,
                    modifier = Modifier.semantics { contentDescription = "Block $caller" },
                )
            }
        }
    }
}
