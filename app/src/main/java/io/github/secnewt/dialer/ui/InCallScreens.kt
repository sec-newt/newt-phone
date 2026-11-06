package io.github.secnewt.dialer.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import io.github.secnewt.dialer.incall.ToneSender
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import io.github.secnewt.ui.NewtTone
import io.github.secnewt.ui.NewtPrimaryButton
import io.github.secnewt.ui.NewtButton
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.disabled
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.secnewt.dialer.calls.AudioRoute
import io.github.secnewt.dialer.calls.AudioState
import io.github.secnewt.dialer.calls.CallPhase
import io.github.secnewt.dialer.calls.Dialpad
import io.github.secnewt.dialer.calls.LiveCall
import io.github.secnewt.dialer.calls.LiveCalls
import io.github.secnewt.dialer.contacts.Contact
import io.github.secnewt.ui.NewtFonts
import kotlinx.coroutines.delay
import io.github.secnewt.ui.neonOutline
import io.github.secnewt.ui.newtContainerColor

/** What the call screen's buttons do. */
data class CallActions(
    val onAnswer: (String) -> Unit = {},
    val onDecline: (String) -> Unit = {},
    val onHangUp: (String) -> Unit = {},
    val onMute: (Boolean) -> Unit = {},
    val onRoute: (AudioRoute) -> Unit = {},
    val onToggleHold: (String) -> Unit = {},
    val onSwap: () -> Unit = {},
    val onMerge: (String) -> Unit = {},
    val onTone: (String, Char) -> Unit = { _, _ -> },
    /** Pasted digits: start a tone and keep it playing until [onToneStop], like a held key. */
    val onToneStart: (String, Char) -> Unit = { _, _ -> },
    val onToneStop: (String) -> Unit = {},
    val onAddCall: () -> Unit = {},
    /** Stops the ringtone and voice; the call keeps ringing silently. */
    val onSilence: (String) -> Unit = {},
    /** Declines the call and opens a text to the caller in the default SMS app. */
    val onTextBack: (String) -> Unit = {},
)

/** The name to show for a call: the contact, the number, "Voicemail" or "Hidden number". */
fun callTitle(call: LiveCall): String = when {
    call.isConference -> "Conference call"
    call.isVoicemail -> "Voicemail"
    call.name != null -> call.name
    else -> formatCaller(call.number)
}

/**
 * The whole call screen: the incoming-call screen while [call] rings, otherwise the
 * in-call controls. [nowMillis] fixes the clock for tests; otherwise the timer ticks.
 */
@Composable
fun CallScreen(
    call: LiveCall,
    other: LiveCall?,
    audio: AudioState,
    actions: CallActions,
    nowMillis: Long? = null,
) {
    val now = nowMillis ?: ticking()
    Surface(modifier = Modifier.fillMaxSize(), color = newtContainerColor()) {
        if (call.phase == CallPhase.RINGING) {
            IncomingCallScreen(call, other, actions, now)
        } else {
            InCallScreen(call, other, audio, actions, now)
        }
    }
}

@Composable
private fun ticking(): Long {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1000)
            value = System.currentTimeMillis()
        }
    }
    return now
}

@Composable
private fun IncomingCallScreen(call: LiveCall, other: LiveCall?, actions: CallActions, now: Long) {
    var silenced by rememberSaveable(call.id) { mutableStateOf(false) }
    val context = incomingContext(call, now)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Incoming call",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = displayFont(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .semantics { heading() },
            )
            CallerHeader(call, avatarSize = 128)
            call.spamLabel?.let { SpamWarning(it) }
            context.forEach { ContextLine(it) }
            if (other != null) {
                Text(
                    "Answering puts ${callTitle(other)} on hold.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // The thumb zone: quick replies, then Decline on its own row, then Answer, the biggest.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                if (call.number != null) {
                    NewtButton("Text back", onClick = { actions.onTextBack(call.id) }, modifier = Modifier.weight(1f).fillMaxHeight())
                }
                if (silenced) {
                    SilencedLabel(Modifier.weight(1f).fillMaxHeight())
                } else {
                    NewtButton(
                        "Silence",
                        onClick = {
                            silenced = true
                            actions.onSilence(call.id)
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
            NewtPrimaryButton(
                "Decline",
                onClick = { actions.onDecline(call.id) },
                icon = Icons.Filled.Close,
                tone = NewtTone.Danger,
                modifier = Modifier.fillMaxWidth(),
            )
            NewtPrimaryButton(
                "Answer",
                onClick = { actions.onAnswer(call.id) },
                icon = Icons.Filled.Call,
                tone = NewtTone.Good,
                large = true,
                modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp),
            )
        }
    }
}

/** One line of who's calling ("★ Favorite · mobile"), on a lifted, outlined row. */
@Composable
private fun ContextLine(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/** Where Silence was: says it worked, and does nothing if tapped again. */
@Composable
private fun SilencedLabel(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .semantics(mergeDescendants = true) {
                role = Role.Button
                disabled()
            },
    ) {
        Text("Silenced", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InCallScreen(call: LiveCall, other: LiveCall?, audio: AudioState, actions: CallActions, now: Long) {
    var keypadOpen by rememberSaveable { mutableStateOf(false) }
    var tones by rememberSaveable(call.id) { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Pasted digits waiting for "Send", and the send in progress. Never added to [tones] or
    // saved: a card number shouldn't sit on the screen.
    var pasted by remember(call.id) { mutableStateOf<String?>(null) }
    var pasteNote by remember(call.id) { mutableStateOf<String?>(null) }
    var sending by remember(call.id) { mutableStateOf<Job?>(null) }
    var sent by remember(call.id) { mutableStateOf(0 to 0) }
    pasted?.let { digits ->
        SendTonesDialog(
            prompt = Dialpad.tonePrompt(digits),
            onSend = {
                pasted = null
                pasteNote = null
                sent = 0 to digits.length
                sending = scope.launch {
                    try {
                        ToneSender.send(
                            digits,
                            start = { actions.onToneStart(call.id, it) },
                            stop = { actions.onToneStop(call.id) },
                            onSent = { sent = it to digits.length },
                        )
                    } finally {
                        sending = null
                    }
                }
            },
            onCancel = { pasted = null },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                LiveCalls.status(call, now),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = displayFont(),
                fontWeight = FontWeight.Bold,
                color = if (call.phase == CallPhase.ENDED) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
                modifier = Modifier.padding(top = 16.dp),
            )
            CallerHeader(call, avatarSize = if (keypadOpen) 72 else 128)
            call.spamLabel?.let { SpamWarning(it) }
            other?.let {
                OtherCallCard(
                    call = it,
                    onSwap = actions.onSwap,
                    onMerge = if (LiveCalls.showMerge(call, it)) ({ actions.onMerge(call.id) }) else null,
                )
            }
            if (keypadOpen) {
                ToneKeypad(
                    tones = tones,
                    status = when {
                        sending != null -> "Sending ${sent.first} of ${sent.second}"
                        else -> pasteNote
                    },
                    sending = sending != null,
                    onTone = { digit ->
                        tones = (tones + digit).takeLast(Dialpad.MAX_LENGTH)
                        actions.onTone(call.id, digit)
                    },
                    onPaste = {
                        val digits = Dialpad.toneDigits(clipboardText(context).orEmpty())
                        when {
                            digits.isEmpty() -> pasteNote = "Nothing to paste. Copy a number first."
                            digits.length > Dialpad.MAX_LENGTH ->
                                pasteNote = "That's more than ${Dialpad.MAX_LENGTH} digits. Copy just the number."
                            else -> pasted = digits
                        }
                    },
                    onStop = { sending?.cancel() },
                    onHide = { keypadOpen = false },
                )
            }
        }
        if (call.phase != CallPhase.ENDED) {
            if (!keypadOpen) {
                Controls(
                    call = call,
                    audio = audio,
                    actions = actions,
                    onOpenKeypad = { keypadOpen = true },
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            BigCallButton(
                label = "End call",
                icon = CallEndIcon,
                container = MaterialTheme.colorScheme.error,
                content = MaterialTheme.colorScheme.onError,
                onClick = { actions.onHangUp(call.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Photo or initial, name, and the number underneath when the name is a contact's. */
@Composable
private fun CallerHeader(call: LiveCall, avatarSize: Int) {
    val title = callTitle(call)
    val avatar = Contact(0, title, starred = false, photoUri = call.photoUri, thumbnailUri = call.photoUri)
    ContactAvatar(
        contact = avatar,
        sizePx = with(LocalDensity.current) { avatarSize.dp.roundToPx() },
        initialStyle = MaterialTheme.typography.displayMedium,
        shape = CircleShape,
        fullPhoto = true,
        modifier = Modifier
            .size(avatarSize.dp)
            .border(neonOutline(3.dp), CircleShape),
    )
    val context = LocalContext.current
    val number = call.number?.takeIf { it.isNotBlank() && !call.isConference }
    val copy = number?.let { n -> Modifier.holdToCopy { copyNumber(context, n) } } ?: Modifier
    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        fontFamily = if (call.name == null && call.number != null) NewtFonts.Mono else bodyFont(),
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = copy,
    )
    if (call.isConference && call.participants.isNotEmpty()) {
        Text(
            text = call.participants.joinToString(", ") { callTitle(it) },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else if (call.name != null && call.number != null) {
        Text(
            text = formatCaller(call.number),
            style = MaterialTheme.typography.titleLarge,
            fontFamily = NewtFonts.Mono,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = copy,
        )
    }
}

@Composable
private fun SpamWarning(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = displayFont(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** The other call: on hold (with Swap, and Merge when the network allows), or still connecting. */
@Composable
private fun OtherCallCard(call: LiveCall, onSwap: () -> Unit, onMerge: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = neonOutline(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PauseIcon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                Text(
                    text = if (call.phase == CallPhase.ON_HOLD) "On hold: ${callTitle(call)}" else callTitle(call),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            if (call.phase == CallPhase.ON_HOLD) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onSwap,
                        border = neonOutline(),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                    ) {
                        Icon(SwapCallsIcon, contentDescription = null, modifier = Modifier.size(22.dp))
                        Text("Swap", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 6.dp))
                    }
                    if (onMerge != null) {
                        OutlinedButton(
                            onClick = onMerge,
                            border = neonOutline(),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                        ) {
                            Icon(MergeIcon, contentDescription = null, modifier = Modifier.size(22.dp))
                            Text("Merge", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Mute, speaker or sound source, keypad, hold and add call: big round buttons with words. */
@Composable
private fun Controls(
    call: LiveCall,
    audio: AudioState,
    actions: CallActions,
    onOpenKeypad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ControlButton(
                icon = if (audio.muted) MicOffIcon else MicIcon,
                label = "Mute",
                checked = audio.muted,
                onClick = { actions.onMute(!audio.muted) },
                modifier = Modifier.weight(1f),
            )
            if (LiveCalls.audioIsSwitch(audio)) {
                ControlButton(
                    icon = routeIcon(audio.route),
                    label = "Sound: ${audio.route.label}",
                    checked = null,
                    onClick = { actions.onRoute(LiveCalls.nextRoute(audio)) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                ControlButton(
                    icon = SpeakerIcon,
                    label = "Speaker",
                    checked = audio.route == AudioRoute.SPEAKER,
                    onClick = { actions.onRoute(LiveCalls.nextRoute(audio)) },
                    modifier = Modifier.weight(1f),
                )
            }
            ControlButton(
                icon = DialpadIcon,
                label = "Keypad",
                checked = null,
                onClick = onOpenKeypad,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ControlButton(
                icon = PauseIcon,
                label = "Hold",
                checked = call.phase == CallPhase.ON_HOLD,
                enabled = call.canHold || call.phase == CallPhase.ON_HOLD,
                onClick = { actions.onToggleHold(call.id) },
                modifier = Modifier.weight(1f),
            )
            ControlButton(
                icon = Icons.Filled.Add,
                label = "Add call",
                checked = null,
                enabled = call.phase == CallPhase.ACTIVE,
                onClick = actions.onAddCall,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

private fun routeIcon(route: AudioRoute): ImageVector = when (route) {
    AudioRoute.EARPIECE -> Icons.Filled.Phone
    AudioRoute.WIRED -> HeadsetIcon
    AudioRoute.SPEAKER -> SpeakerIcon
    AudioRoute.BLUETOOTH -> BluetoothIcon
}

/**
 * A round control with its name underneath. On/off controls ([checked] not null) light up
 * with the outline gradient when on, like a pressed dialpad key.
 */
@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    checked: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val on = checked == true
    val fill: Brush = if (on) Brush.linearGradient(listOf(colors.primary, colors.secondary)) else SolidColor(colors.surfaceVariant)
    val action = if (checked != null) {
        Modifier.toggleable(value = on, enabled = enabled, role = Role.Switch, onValueChange = { onClick() })
    } else {
        Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    }
    val contentColor = when {
        !enabled -> colors.onSurfaceVariant
        on -> colors.onPrimary
        else -> colors.onSurface
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(action)
            .padding(vertical = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(fill)
                .border(neonOutline(), CircleShape),
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(32.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (enabled) colors.onSurface else colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** A wide, tall button with an icon and a word: Answer, Decline, End call. */
@Composable
private fun BigCallButton(
    label: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.heightIn(min = 80.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(34.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

/**
 * The dialpad during a call: each key sends its tone (for menus like "press 1 for…"). Paste
 * sends copied digits, such as a card number from a password manager, after asking first.
 */
@Composable
private fun ToneKeypad(
    tones: String,
    status: String?,
    sending: Boolean,
    onTone: (Char) -> Unit,
    onPaste: () -> Unit,
    onStop: () -> Unit,
    onHide: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (status != null) {
            Text(
                status,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        } else if (tones.isNotEmpty()) {
            Text(
                tones,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = NewtFonts.Mono,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Dialpad.keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Key(
                        key = key,
                        onPress = { onTone(key.digit) },
                        onLongPress = { onTone(key.digit) },
                        label = Dialpad.toneLabel(key),
                        longPressLabel = null,
                        showVoicemail = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (sending) {
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                ) {
                    Text("Stop sending", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onPaste,
                    border = neonOutline(),
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                ) {
                    Icon(PasteIcon, contentDescription = null, modifier = Modifier.size(24.dp))
                    Text(
                        "Paste",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            TextButton(onClick = onHide, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                Text("Hide keypad", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Asks before sending pasted digits, showing only how many and the last four. */
@Composable
private fun SendTonesDialog(prompt: String, onSend: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(prompt, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "They're sent as key presses, the same as typing them on the keypad.",
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            TextButton(onClick = onSend, modifier = Modifier.heightIn(min = 56.dp)) {
                Text("Send", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 56.dp)) {
                Text("Cancel", style = MaterialTheme.typography.titleLarge)
            }
        },
    )
}

/** Hold to copy, with Android's own "Copied" message; a screen reader offers it as an action. */
private fun Modifier.holdToCopy(onCopy: () -> Unit): Modifier = this
    .heightIn(min = 48.dp)
    .pointerInput(onCopy) { detectTapGestures(onLongPress = { onCopy() }) }
    .semantics { onLongClick(label = "Copy number") { onCopy(); true } }
    .wrapContentHeight()

/** A green strip on the main screens while a call is going, to get back to it. */
@Composable
fun ReturnToCallBar(call: LiveCall, onClick: () -> Unit) {
    val now = ticking()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(CallGreen)
            .clickable(role = Role.Button, onClick = onClick)
            .windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = 56.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Icon(Icons.Filled.Call, contentDescription = null, tint = OnCallGreen, modifier = Modifier.size(24.dp))
        Text(
            "Return to call: ${callTitle(call)}, ${LiveCalls.status(call, now)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnCallGreen,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
