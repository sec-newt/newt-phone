package io.github.secnewt.dialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.secnewt.dialer.contacts.Contact
import io.github.secnewt.dialer.contacts.ContactList
import io.github.secnewt.dialer.contacts.Dnd
import io.github.secnewt.dialer.contacts.DndCalls
import io.github.secnewt.dialer.contacts.PhoneEntry

/** Starred contacts, with one-tap calling and whether they can ring during Do Not Disturb. */
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
) {
    val favorites = ContactList.favorites(contacts)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { TabHeader("Favorites", onOpenSettings) }
            message?.let { item { MessageText(it) } }
            if (!hasAccess) {
                item { ContactsAccessCard(onAllowAccess) }
                return@LazyColumn
            }
            item { DndCard(dnd, onOpenDndSettings) }
            if (favorites.isEmpty()) {
                item {
                    Text(
                        text = "No favorites yet. In Contacts, tap the star next to a name to add it here.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(favorites, key = { it.id }) { contact ->
                    FavoriteRow(contact, onOpen = { onOpenContact(contact) }, onCall = onCall)
                    HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun FavoriteRow(contact: Contact, onOpen: () -> Unit, onCall: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClickLabel = "Open", onClick = onOpen),
    ) {
        Initial(contact.name)
        Text(
            text = contact.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        )
        contact.phones.firstOrNull()?.let { phone ->
            FilledTonalIconButton(onClick = { onCall(phone.number) }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.Call, contentDescription = "Call ${contact.name}", modifier = Modifier.size(28.dp))
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
) {
    val shown = ContactList.search(contacts, query)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                    ContactRow(contact, onOpen = { onOpenContact(contact) }, onToggleStar = { onToggleStar(contact) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: Contact, onOpen: () -> Unit, onToggleStar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClickLabel = "Open", onClick = onOpen)
            .padding(vertical = 4.dp),
    ) {
        Initial(contact.name)
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
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (contact.starred) {
                        OutlinedButton(
                            onClick = onToggleStar,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(24.dp))
                            Text(
                                "In favorites. Tap to remove",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    } else {
                        Button(
                            onClick = onToggleStar,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                        ) {
                            Icon(StarOutline, contentDescription = null, modifier = Modifier.size(24.dp))
                            Text(
                                "Add to favorites",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                    Dnd.contactLine(contact.starred, dnd)?.let {
                        Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (contact.phones.isEmpty()) {
                item {
                    Text(
                        text = "No phone number saved.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
            items(contact.phones) { phone -> PhoneRow(phone, onCall) }
        }
    }
}

@Composable
private fun PhoneRow(phone: PhoneEntry, onCall: (String) -> Unit) {
    val formatted = formatCaller(phone.number)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(formatted, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                phone.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalButton(
            onClick = { onCall(phone.number) },
            modifier = Modifier
                .heightIn(min = 56.dp)
                .semantics { contentDescription = "Call ${phone.label}, $formatted" },
        ) {
            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(24.dp))
            Text("Call", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun DndCard(dnd: DndCalls, onOpenDndSettings: () -> Unit) {
    val advice = Dnd.advice(dnd)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Do Not Disturb",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Text(advice.message, style = MaterialTheme.typography.bodyLarge)
            if (advice.suggestSettings) {
                OutlinedButton(
                    onClick = onOpenDndSettings,
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

/** A round badge with the first letter of the name. Decorative: the name is read right after it. */
@Composable
private fun Initial(name: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clearAndSetSemantics { },
    ) {
        Text(
            text = name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "#",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
