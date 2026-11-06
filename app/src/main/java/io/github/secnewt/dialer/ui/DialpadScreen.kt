package io.github.secnewt.dialer.ui

import android.telephony.PhoneNumberUtils
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.TextButton
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import io.github.secnewt.dialer.calls.DialText
import kotlinx.coroutines.awaitCancellation
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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import io.github.secnewt.ui.NewtFonts
import java.util.Locale
import io.github.secnewt.ui.neonOutline
import io.github.secnewt.ui.newtContainerColor

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
    onKeyTone: (Char) -> Unit = {},
    onNumberAction: (NumberAction, String) -> Unit = { _, _ -> },
) {
    val matches = remember(contacts, number) { ContactList.dialpadMatches(contacts, number) }
    // Where the cursor is. When the number changes from outside (a paste from the menu, a
    // contact picked, "Edit before calling"), the cursor goes to the end.
    var dial by remember { mutableStateOf(DialText(number)) }
    val current = if (dial.text == number) dial else DialText(number)
    val edit: (DialText) -> Unit = { dial = it; onNumberChange(it.text) }
    Surface(modifier = Modifier.fillMaxSize(), color = newtContainerColor()) {
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
                        fontFamily = displayFont(),
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
                NumberRow(current, edit, onBack)
                Keypad(current, edit, onVoicemail, onKeyTone)
                CallRow(number, onCall, onNumberAction)
            }
        }
    }
}

/**
 * The typed number, in a real text field: tap to put the cursor anywhere, drag to select, and
 * hold for Android's own Cut, Copy and Paste. The keys type at the cursor. The on-screen
 * keyboard never opens; the dialpad is the keyboard.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NumberRow(
    dial: DialText,
    onEdit: (DialText) -> Unit,
    onBack: () -> Unit,
) {
    val shown = formatTyped(dial.text)
    val base = if (shown.length > 14) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close dialpad")
        }
        // Swallows the request for a keyboard, so the field keeps its cursor and selection
        // without the keyboard covering the dialpad.
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
        // Shrinks a long number to fit, so the whole number shows even at large text sizes.
        // A JetBrains Mono character is 0.6 of the font size wide.
        val fit = with(LocalDensity.current) { (maxWidth / ((shown.length + 1) * 0.6f)).toSp() }
        val style = if (fit < base.fontSize) base.copy(fontSize = fit) else base
        InterceptPlatformTextInput(interceptor = { _, _ -> awaitCancellation() }) {
            BasicTextField(
                value = TextFieldValue(dial.text, TextRange(dial.start, dial.end)),
                onValueChange = { onEdit(Dialpad.cleanEdit(it.text, it.selection.start, it.selection.end)) },
                singleLine = true,
                textStyle = style.copy(
                    fontFamily = NewtFonts.Mono,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = TypedNumberFormat,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        if (dial.text.isEmpty()) {
                            Text(
                                "Enter a number",
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = bodyFont(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.clearAndSetSemantics {},
                            )
                        }
                        field()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .wrapContentHeight()
                    .semantics {
                        contentDescription = "Number to call"
                        liveRegion = LiveRegionMode.Polite
                    },
            )
        }
        }
        DeleteButton(dial, onEdit)
    }
}

/** Shows the typed number formatted ("(555) 019-77") while the cursor still moves by digit. */
private object TypedNumberFormat : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val typed = text.text
        val formatted = formatTyped(typed)
        val positions = Dialpad.formatPositions(typed, formatted)
            ?: return TransformedText(text, OffsetMapping.Identity)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset < typed.length) positions[offset] else formatted.length

            override fun transformedToOriginal(offset: Int): Int = positions.count { it < offset }
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

/** "(555) 019-77" while typing US numbers; anything else shows as typed. */
private fun formatTyped(number: String): String {
    if (number.isEmpty()) return number
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
                fontFamily = NewtFonts.Mono,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalIconButton(onClick = onCall, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Filled.Call, contentDescription = "Call ${contact.name}, $formatted", modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun Keypad(
    dial: DialText,
    onEdit: (DialText) -> Unit,
    onVoicemail: () -> Unit,
    onKeyTone: (Char) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Dialpad.keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Key(
                        key = key,
                        onPress = {
                            onKeyTone(key.digit)
                            onEdit(Dialpad.type(dial, key.digit.toString()))
                        },
                        onLongPress = {
                            if (Dialpad.holdCallsVoicemail(dial.text, key.digit)) {
                                onVoicemail()
                            } else {
                                onEdit(Dialpad.type(dial, if (key.digit == '0') "+" else key.digit.toString()))
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
internal fun Key(
    key: DialKey,
    onPress: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = Dialpad.spokenLabel(key),
    longPressLabel: String? = when (key.digit) {
        '0' -> "Type plus"
        '1' -> "Call voicemail"
        else -> null
    },
    showVoicemail: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(18.dp)
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
                longPressLabel?.let { onLongClick(label = it) { onLongPress(); true } }
            }
            .padding(vertical = 6.dp),
    ) {
        Text(
            key.digit.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = displayFont(),
            fontWeight = FontWeight.Bold,
            color = digitColor,
        )
        if (key.digit == '1' && showVoicemail) {
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

/** The green Call button, with "More" beside it: paste, copy, save or text the number. */
@Composable
private fun CallRow(number: String, onCall: (String) -> Unit, onNumberAction: (NumberAction, String) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    if (menuOpen) {
        NumberActionsDialog(
            title = if (number.isEmpty()) "Dialpad" else formatTyped(number),
            number = null,
            actions = dialpadActions(hasNumber = number.isNotEmpty()),
            onAction = { action ->
                menuOpen = false
                onNumberAction(action, number)
            },
            onDismiss = { menuOpen = false },
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.weight(1f)) {
            TextButton(onClick = { menuOpen = true }, modifier = Modifier.heightIn(min = 56.dp)) {
                Icon(Icons.Filled.MoreVert, contentDescription = null, modifier = Modifier.size(26.dp))
                Text("More", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
            }
        }
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
        Spacer(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeleteButton(dial: DialText, onEdit: (DialText) -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
        if (dial.text.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = { onEdit(Dialpad.backspace(dial)) },
                        onLongClick = { onEdit(DialText("")) },
                    )
                    .clearAndSetSemantics {
                        contentDescription = "Delete last digit"
                        role = Role.Button
                        onClick { onEdit(Dialpad.backspace(dial)); true }
                        onLongClick(label = "Clear number") { onEdit(DialText("")); true }
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
