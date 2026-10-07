package io.github.secnewt.dialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import io.github.secnewt.dialer.ui.theme.DialerFonts
import kotlinx.coroutines.delay

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
    val onAddCall: () -> Unit = {},
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
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (call.phase == CallPhase.RINGING) {
            IncomingCallScreen(call, other, actions)
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
private fun IncomingCallScreen(call: LiveCall, other: LiveCall?, actions: CallActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Incoming call",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = DialerFonts.Display,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .semantics { heading() },
            )
            CallerHeader(call, avatarSize = 168)
            call.spamLabel?.let { SpamWarning(it) }
            if (other != null) {
                Text(
                    "Answering puts ${callTitle(other)} on hold.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 16.dp)) {
            BigCallButton(
                label = "Decline",
                icon = CallEndIcon,
                container = MaterialTheme.colorScheme.error,
                content = MaterialTheme.colorScheme.onError,
                onClick = { actions.onDecline(call.id) },
                modifier = Modifier.weight(1f),
            )
            BigCallButton(
                label = "Answer",
                icon = Icons.Filled.Call,
                container = CallGreen,
                content = OnCallGreen,
                onClick = { actions.onAnswer(call.id) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InCallScreen(call: LiveCall, other: LiveCall?, audio: AudioState, actions: CallActions, now: Long) {
    var keypadOpen by rememberSaveable { mutableStateOf(false) }
    var tones by rememberSaveable(call.id) { mutableStateOf("") }
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
                fontFamily = DialerFonts.Display,
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
                    onTone = { digit ->
                        tones = (tones + digit).takeLast(Dialpad.MAX_LENGTH)
                        actions.onTone(call.id, digit)
                    },
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
    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        fontFamily = if (call.name == null && call.number != null) DialerFonts.Mono else DialerFonts.Body,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
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
            fontFamily = DialerFonts.Mono,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            fontFamily = DialerFonts.Display,
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

/** The dialpad during a call: each key sends its tone (for menus like "press 1 for…"). */
@Composable
private fun ToneKeypad(tones: String, onTone: (Char) -> Unit, onHide: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (tones.isNotEmpty()) {
            Text(
                tones,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = DialerFonts.Mono,
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
        TextButton(onClick = onHide, modifier = Modifier.heightIn(min = 52.dp)) {
            Text("Hide keypad", style = MaterialTheme.typography.titleMedium)
        }
    }
}

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
