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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
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

/** Big-key dialpad. Typing 3+ digits shows matching contacts above the keys. */
@Composable
fun DialpadScreen(
    number: String,
    onNumberChange: (String) -> Unit,
    contacts: List<Contact>,
    onCall: (String) -> Unit,
    onBack: () -> Unit,
) {
    val digits = number.filter { it.isDigit() }
    val matches = if (digits.length >= 3) ContactList.search(contacts, digits).take(3) else emptyList()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader("Dialpad", onBack)
            NumberDisplay(number)
            matches.forEach { contact ->
                MatchRow(contact) { contact.phones.firstOrNull()?.let { onNumberChange(Dialpad.clean(it.number)) } }
            }
            Keypad(number, onNumberChange)
            CallRow(number, onNumberChange, onCall)
        }
    }
}

@Composable
private fun NumberDisplay(number: String) {
    val shown = if (number.isEmpty()) "" else formatTyped(number)
    Text(
        text = shown.ifEmpty { "Enter a number" },
        style = if (shown.length > 14) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
        fontFamily = if (shown.isEmpty()) DialerFonts.Body else DialerFonts.Mono,
        fontWeight = FontWeight.Bold,
        color = if (shown.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(vertical = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
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
private fun MatchRow(contact: Contact, onPick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClickLabel = "Use this number", onClick = onPick),
    ) {
        ContactAvatar(
            contact = contact,
            sizePx = with(LocalDensity.current) { 40.dp.roundToPx() },
            initialStyle = MaterialTheme.typography.titleMedium,
            shape = CircleShape,
            modifier = Modifier.size(40.dp),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            contact.phones.firstOrNull()?.let {
                Text(
                    formatCaller(it.number),
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = DialerFonts.Mono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Keypad(number: String, onNumberChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Dialpad.keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    Key(
                        key = key,
                        onPress = { onNumberChange(Dialpad.press(number, key.digit)) },
                        onLongPress = { onNumberChange(Dialpad.longPress(number, key.digit)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Key(key: DialKey, onPress: () -> Unit, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(18.dp)
    val label = Dialpad.spokenLabel(key)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .combinedClickable(
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
                if (key.digit == '0') onLongClick(label = "Type plus") { onLongPress(); true }
            }
            .padding(vertical = 6.dp),
    ) {
        Text(
            key.digit.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = DialerFonts.Display,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (key.letters.isNotEmpty()) {
            Text(
                key.letters,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallRow(number: String, onNumberChange: (String) -> Unit, onCall: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
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
                modifier = Modifier.size(36.dp),
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (number.isNotEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
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
}
