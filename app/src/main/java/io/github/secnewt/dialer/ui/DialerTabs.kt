package io.github.secnewt.dialer.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight

enum class Tab(val label: String, val icon: ImageVector) {
    CALLS("Calls", Icons.Filled.Phone),
    FAVORITES("Favorites", Icons.Filled.Star),
    CONTACTS("Contacts", Icons.Filled.Person),
}

/** Bottom navigation, always showing both the icon and the word. */
@Composable
fun DialerTabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = {
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (tab == selected) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                alwaysShowLabel = true,
            )
        }
    }
}
