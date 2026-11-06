package io.github.secnewt.dialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.secnewt.ui.NewtTone
import io.github.secnewt.ui.color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.secnewt.dialer.contacts.Contact
import io.github.secnewt.dialer.contacts.ContactList
import io.github.secnewt.dialer.contacts.Dnd
import io.github.secnewt.dialer.contacts.DndCalls
import io.github.secnewt.dialer.contacts.PhoneEntry
import io.github.secnewt.ui.NewtFonts
import io.github.secnewt.ui.ScreenHeader
import io.github.secnewt.ui.NewtPrimaryButton
import io.github.secnewt.ui.NewtButton
import io.github.secnewt.ui.NewtSwitchRow
import io.github.secnewt.ui.NewtSettingsCard
import io.github.secnewt.ui.CardTitle
import io.github.secnewt.ui.neonOutline
import io.github.secnewt.ui.neonFieldColors
import io.github.secnewt.ui.TabHeader
import io.github.secnewt.ui.StarOutline
import io.github.secnewt.ui.newtContainerColor

/** Starred contacts as a grid of photo tiles: tap a tile to call, or the corner button for details. */
@Composable
fun FavoritesScreen(
    contacts: List<Contact>,
    hasAccess: Boolean,
    dnd: DndCalls,
    onOpenSettings: () -> Unit,
    onAllowAccess: () -> Unit,
    onOpenContact: (Contact) -> Unit,
    onCall: (String) -> Unit,
    onOpenDndSettings: () -> Unit,
    message: String? = null,
    onContactAction: (NumberAction, Contact, String?) -> Unit = { _, _, _ -> },
) {
    val favorites = ContactList.favorites(contacts)
    val fullWidth: (LazyGridItemSpanScope) -> GridItemSpan = { GridItemSpan(it.maxLineSpan) }
    Surface(modifier = Modifier.fillMaxSize(), color = newtContainerColor()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = fullWidth) { TabHeader("Favorites", onOpenSettings) }
            message?.let { item(span = fullWidth) { MessageText(it) } }
            if (!hasAccess) {
                item(span = fullWidth) { ContactsAccessCard(onAllowAccess) }
                return@LazyVerticalGrid
            }
            item(span = fullWidth) { DndCard(dnd, onOpenDndSettings) }
            if (favorites.isEmpty()) {
                item(span = fullWidth) {
                    Text(
                        text = "No favorites yet. In Contacts, tap the star next to a name to add it here.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                gridItems(favorites, key = { it.id }) { contact ->
                    FavoriteTile(
                        contact,
                        onOpen = { onOpenContact(contact) },
                        onCall = onCall,
                        onAction = { action, number -> onContactAction(action, contact, number) },
                    )
                }
            }
        }
    }
}

/** Readable over any photo: white text on a nearly opaque dark band. */
private val TileScrim = Color(0xE6050608)
private val TileText = Color(0xFFF2F4FF)

/** Tap to call; hold for more (edit the contact, copy or text the number). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteTile(
    contact: Contact,
    onOpen: () -> Unit,
    onCall: (String) -> Unit,
    onAction: (NumberAction, String?) -> Unit,
) {
    val number = contact.phones.firstOrNull()?.number
    val shape = RoundedCornerShape(16.dp)
    val density = LocalDensity.current
    var menuOpen by remember { mutableStateOf(false) }
    if (menuOpen) ContactMenu(contact, number, onAction) { menuOpen = false }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .border(2.dp, contactColor(contact.name), shape)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = if (number != null) "Call" else "Open",
                onLongClickLabel = "More options",
                onLongClick = { menuOpen = true },
                onClick = { if (number != null) onCall(number) else onOpen() },
            ),
    ) {
        ContactAvatar(
            contact = contact,
            sizePx = with(density) { 200.dp.roundToPx() },
            initialStyle = MaterialTheme.typography.displayLarge,
            fullPhoto = true,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = contact.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TileText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(TileScrim)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        IconButton(
            onClick = onOpen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(48.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TileScrim),
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Open ${contact.name}'s contact",
                    tint = TileText,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/** Every contact, searchable, with a star to add or remove favorites. */
@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    hasAccess: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onAllowAccess: () -> Unit,
    onOpenContact: (Contact) -> Unit,
    onToggleStar: (Contact) -> Unit,
    message: String? = null,
    onContactAction: (NumberAction, Contact, String?) -> Unit = { _, _, _ -> },
) {
    val shown = ContactList.search(contacts, query)
    Surface(modifier = Modifier.fillMaxSize(), color = newtContainerColor()) {
        LazyColumn(
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TabHeader("Contacts", onOpenSettings) }
            message?.let { item { MessageText(it) } }
            if (!hasAccess) {
                item { ContactsAccessCard(onAllowAccess) }
                return@LazyColumn
            }
            item {
                OutlinedTextField(
                    colors = neonFieldColors(),
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Search by name or number") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
            }
            if (shown.isEmpty()) {
                item {
                    Text(
                        text = if (contacts.isEmpty()) "No contacts on this phone yet." else "No matches.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(shown, key = { it.id }) { contact ->
                    ContactRow(
                        contact,
                        onOpen = { onOpenContact(contact) },
                        onToggleStar = { onToggleStar(contact) },
                        onAction = { action, number -> onContactAction(action, contact, number) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

/** Tap to open; hold for more (edit the contact, copy or text the number). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactRow(
    contact: Contact,
    onOpen: () -> Unit,
    onToggleStar: () -> Unit,
    onAction: (NumberAction, String?) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    if (menuOpen) ContactMenu(contact, contact.phones.firstOrNull()?.number, onAction) { menuOpen = false }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = "Open",
                onLongClickLabel = "More options",
                onLongClick = { menuOpen = true },
                onClick = onOpen,
            )
            .padding(vertical = 4.dp),
    ) {
        ContactAvatar(
            contact = contact,
            sizePx = with(LocalDensity.current) { 52.dp.roundToPx() },
            initialStyle = MaterialTheme.typography.titleLarge,
            shape = CircleShape,
            modifier = Modifier.size(52.dp),
        )
        Text(
            text = contact.name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        )
        StarButton(contact, onToggleStar)
    }
}

@Composable
private fun ContactMenu(
    contact: Contact,
    number: String?,
    onAction: (NumberAction, String?) -> Unit,
    onClose: () -> Unit,
) {
    NumberActionsDialog(
        title = contact.name,
        number = number,
        actions = contactActions(hasNumber = number != null),
        onAction = { action ->
            onClose()
            onAction(action, number)
        },
        onDismiss = onClose,
    )
}

@Composable
private fun StarButton(contact: Contact, onToggle: () -> Unit) {
    IconButton(onClick = onToggle, modifier = Modifier.size(56.dp)) {
        Icon(
            imageVector = if (contact.starred) Icons.Filled.Star else StarOutline,
            contentDescription = if (contact.starred) {
                "Remove ${contact.name} from favorites"
            } else {
                "Add ${contact.name} to favorites"
            },
            tint = if (contact.starred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp),
        )
    }
}

/** One contact: favorite on or off, Do Not Disturb status, and each number with a Call button. */
@Composable
fun ContactDetailScreen(
    contact: Contact,
    dnd: DndCalls,
    onBack: () -> Unit,
    onToggleStar: () -> Unit,
    onCall: (String) -> Unit,
    onContactAction: (NumberAction, Contact, String?) -> Unit = { _, _, _ -> },
    isBlocked: (String) -> Boolean = { false },
) {
    Surface(modifier = Modifier.fillMaxSize(), color = newtContainerColor()) {
        LazyColumn(
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { ScreenHeader("Contact", onBack) }
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    ContactAvatar(
                        contact = contact,
                        sizePx = with(LocalDensity.current) { 120.dp.roundToPx() },
                        initialStyle = MaterialTheme.typography.displayMedium,
                        shape = CircleShape,
                        fullPhoto = true,
                        modifier = Modifier
                            .size(120.dp)
                            .border(3.dp, contactColor(contact.name), CircleShape),
                    )
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = bodyFont(),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                }
            }
            item {
                NewtSettingsCard("Ringing", icon = Icons.Filled.Star) {
                    NewtSwitchRow(
                        "Favorite",
                        checked = contact.starred,
                        onCheckedChange = { onToggleStar() },
                        hint = Dnd.contactLine(contact.starred, dnd),
                    )
                }
            }
            item {
                NewtSettingsCard("Numbers", icon = Icons.Filled.Call) {
                    if (contact.phones.isEmpty()) {
                        Text(
                            text = "No phone number saved.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                    contact.phones.forEachIndexed { index, phone ->
                        if (index > 0) HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        PhoneRow(phone, onCall, isBlocked(phone.number)) { action -> onContactAction(action, contact, phone.number) }
                    }
                }
            }
            item {
                NewtButton(
                    "Edit in Contacts",
                    onClick = { onContactAction(NumberAction.EDIT_CONTACT, contact, null) },
                    icon = Icons.Filled.Edit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * One number on a contact's page: tap or hold the number to copy, edit or block it. Under it,
 * a green Call and a cyan Text button, outlined like the Call button in Recents.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneRow(phone: PhoneEntry, onCall: (String) -> Unit, isBlocked: Boolean, onAction: (NumberAction) -> Unit) {
    val formatted = formatCaller(phone.number)
    var menuOpen by remember { mutableStateOf(false) }
    if (menuOpen) {
        NumberActionsDialog(
            title = formatted,
            number = null,
            actions = phoneActions(isBlocked),
            onAction = { action ->
                menuOpen = false
                onAction(action)
            },
            onDismiss = { menuOpen = false },
        )
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .combinedClickable(
                    onClickLabel = "More options",
                    onLongClickLabel = "More options",
                    onLongClick = { menuOpen = true },
                    onClick = { menuOpen = true },
                ),
        ) {
            Text(
                formatted,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = NewtFonts.Mono,
                fontWeight = FontWeight.Bold,
            )
            Text(
                phone.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedActionButton(
                label = "Call",
                icon = Icons.Filled.Call,
                color = NewtTone.Good.color(),
                spoken = "Call ${phone.label}, $formatted",
                onClick = { onCall(phone.number) },
                modifier = Modifier.weight(1f),
            )
            OutlinedActionButton(
                label = "Text",
                icon = MessageIcon,
                color = MaterialTheme.colorScheme.tertiary,
                spoken = "Text ${phone.label}, $formatted",
                onClick = { onAction(NumberAction.MESSAGE) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** A big outlined button in its own color, with an icon and a word. */
@Composable
private fun OutlinedActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    spoken: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 58.dp)
            .clip(shape)
            .border(2.5.dp, color, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick { onClick(); true }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun DndCard(dnd: DndCalls, onOpenDndSettings: () -> Unit) {
    val advice = Dnd.advice(dnd)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = neonOutline(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardTitle("Do Not Disturb")
            Text(advice.message, style = MaterialTheme.typography.bodyLarge)
            if (advice.suggestSettings) {
                OutlinedButton(
                    onClick = onOpenDndSettings,
                    border = neonOutline(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text("Open Do Not Disturb settings", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun ContactsAccessCard(onAllowAccess: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = neonOutline(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Your contacts stay on this phone. The app needs access to show them and to star favorites, " +
                    "which Do Not Disturb uses to decide who can ring.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                onClick = onAllowAccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text("Allow contacts access", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun MessageText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
}
