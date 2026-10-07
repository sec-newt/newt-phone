package io.github.secnewt.dialer.ui

import android.telephony.PhoneNumberUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.heading
import io.github.secnewt.dialer.contacts.PhoneEntry
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.secnewt.dialer.calls.DialKey
import io.github.secnewt.dialer.calls.Dialpad
import io.github.secnewt.dialer.contacts.Contact
import io.github.secnewt.dialer.contacts.ContactList
import io.github.secnewt.dialer.ui.theme.DialerFonts
import java.util.Locale

/**
 * Dialpad laid out like the stock one: a scrolling list of contacts on top (favorites first,
 * then everyone whose number matches what's typed), the number with back and delete, then the
 * keys and the Call button fixed at the bottom.
 */
@Composable
fun DialpadScreen(
    number: String,
    onNumberChange: (String) -> Unit,
    contacts: List<Contact>,
    onCall: (String) -> Unit,
    onBack: () -> Unit,
    onVoicemail: () -> Unit = {},
) {
    val matches = remember(contacts, number) { ContactList.dialpadMatches(contacts, number) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                item {
                    Text(
                        text = when {
                            number.isEmpty() && matches.isEmpty() -> "Type a number"
                            number.isEmpty() -> "Favorites"
                            matches.isEmpty() -> "No matching contacts"
                            else -> "Matching contacts"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = DialerFonts.Display,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .semantics { heading() },
                    )
                }
                items(matches, key = { it.first.id }) { (contact, phone) ->
                    MatchRow(
                        contact = contact,
                        phone = phone,
                        onPick = { onNumberChange(Dialpad.clean(phone.number)) },
                        onCall = { onCall(phone.number) },
                    )
                }
            }
            HorizontalDivider()
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NumberRow(number, onNumberChange, onBack)
                Keypad(number, onNumberChange, onVoicemail)
                CallButton(number, onCall)
            }
        }
    }
}

@Composable
private fun NumberRow(number: String, onNumberChange: (String) -> Unit, onBack: () -> Unit) {
    val shown = if (number.isEmpty()) "" else formatTyped(number)
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close dialpad")
        }
        Text(
            text = shown.ifEmpty { "Enter a number" },
            style = if (shown.length > 14) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge,
            fontFamily = if (shown.isEmpty()) DialerFonts.Body else DialerFonts.Mono,
            fontWeight = FontWeight.Bold,
            color = if (shown.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        DeleteButton(number, onNumberChange)
    }
}

/** "(555) 019-77" while typing US numbers; anything else shows as typed. */
private fun formatTyped(number: String): String {
    val country = Locale.getDefault().country.ifEmpty { "US" }
    return try {
        PhoneNumberUtils.formatNumber(number, country) ?: number
    } catch (e: Exception) {
        number
    }
}

@Composable
private fun MatchRow(contact: Contact, phone: PhoneEntry, onPick: () -> Unit, onCall: () -> Unit) {
    val formatted = formatCaller(phone.number)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClickLabel = "Use this number", onClick = onPick)
            .padding(vertical = 4.dp),
    ) {
        ContactAvatar(
            contact = contact,
            sizePx = with(LocalDensity.current) { 48.dp.roundToPx() },
            initialStyle = MaterialTheme.typography.titleLarge,
            shape = CircleShape,
            modifier = Modifier.size(48.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(contact.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                formatted,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = DialerFonts.Mono,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalIconButton(onClick = onCall, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Filled.Call, contentDescription = "Call ${contact.name}, $formatted", modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun Keypad(number: String, onNumberChange: (String) -> Unit, onVoicemail: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Dialpad.keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Key(
                        key = key,
                        onPress = { onNumberChange(Dialpad.press(number, key.digit)) },
                        onLongPress = {
                            if (Dialpad.holdCallsVoicemail(number, key.digit)) {
                                onVoicemail()
                            } else {
                                onNumberChange(Dialpad.longPress(number, key.digit))
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * A dialpad key. While pressed it lights up with the same blue-to-purple gradient as the
 * outlines, and the digit turns dark so it stays easy to read.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Key(key: DialKey, onPress: () -> Unit, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(18.dp)
    val label = Dialpad.spokenLabel(key)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val colors = MaterialTheme.colorScheme
    val fill: Brush = if (pressed) {
        Brush.linearGradient(listOf(colors.primary, colors.secondary))
    } else {
        SolidColor(colors.surfaceVariant)
    }
    val digitColor = if (pressed) colors.onPrimary else colors.onSurface
    val subColor = if (pressed) colors.onPrimary else colors.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 62.dp)
            .clip(shape)
            .background(fill)
            .border(neonOutline(), shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPress()
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                },
            )
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                onClick { onPress(); true }
                when (key.digit) {
                    '0' -> onLongClick(label = "Type plus") { onLongPress(); true }
                    '1' -> onLongClick(label = "Call voicemail") { onLongPress(); true }
                }
            }
            .padding(vertical = 6.dp),
    ) {
        Text(
            key.digit.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = DialerFonts.Display,
            fontWeight = FontWeight.Bold,
            color = digitColor,
        )
        if (key.digit == '1') {
            Icon(VoicemailIcon, contentDescription = null, tint = subColor, modifier = Modifier.size(22.dp))
        } else if (key.letters.isNotEmpty()) {
            Text(
                key.letters,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = subColor,
            )
        }
    }
}

@Composable
private fun CallButton(number: String, onCall: (String) -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(if (number.isEmpty()) MaterialTheme.colorScheme.surfaceVariant else CallGreen)
                .clickable(
                    enabled = number.isNotEmpty(),
                    role = Role.Button,
                    onClickLabel = "Call",
                    onClick = { onCall(number) },
                )
                .semantics { contentDescription = "Call" },
        ) {
            Icon(
                Icons.Filled.Call,
                contentDescription = null,
                tint = if (number.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else OnCallGreen,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeleteButton(number: String, onNumberChange: (String) -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
        if (number.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = { onNumberChange(Dialpad.backspace(number)) },
                        onLongClick = { onNumberChange("") },
                    )
                    .clearAndSetSemantics {
                        contentDescription = "Delete last digit"
                        role = Role.Button
                        onClick { onNumberChange(Dialpad.backspace(number)); true }
                        onLongClick(label = "Clear number") { onNumberChange(""); true }
                    },
            ) {
                Icon(
                    BackspaceIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}
