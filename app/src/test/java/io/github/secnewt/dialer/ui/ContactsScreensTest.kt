package io.github.secnewt.dialer.ui

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import io.github.secnewt.dialer.contacts.Contact
import io.github.secnewt.dialer.contacts.DndCalls
import io.github.secnewt.dialer.contacts.PhoneEntry
import io.github.secnewt.dialer.ui.theme.DialerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Screenshots, accessibility checks and taps for Favorites, Contacts and a contact's page. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
@OptIn(ExperimentalRoborazziApi::class)
class ContactsScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val contacts = listOf(
        Contact(1, "Mom", starred = true, phones = listOf(PhoneEntry("5550197731", "Mobile"))),
        Contact(
            2, "Alex Rivera", starred = true,
            phones = listOf(PhoneEntry("5550142290", "Mobile"), PhoneEntry("5550148800", "Work")),
        ),
        Contact(3, "Dr. Patel's office", starred = false, phones = listOf(PhoneEntry("5550161234", "Work"))),
        Contact(4, "Pharmacy", starred = false, phones = listOf(PhoneEntry("8005550000", "Main"))),
    )

    private fun render(name: String, dark: Boolean, fontScale: Float = 1f, content: @Composable () -> Unit) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DialerTheme(darkTheme = dark) { content() }
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/$name.png")
        composeRule.onRoot().checkRoboAccessibility(
            RoborazziATFAccessibilityCheckOptions(
                failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Warning,
            )
        )
    }

    @Composable
    private fun WithTabs(tab: Tab, content: @Composable () -> Unit) {
        Scaffold(bottomBar = { DialerTabBar(selected = tab, onSelect = {}) }) { padding ->
            Box(Modifier.padding(padding)) { content() }
        }
    }

    @Composable
    private fun Favorites(dnd: DndCalls, list: List<Contact> = contacts, hasAccess: Boolean = true) =
        FavoritesScreen(
            contacts = list,
            hasAccess = hasAccess,
            dnd = dnd,
            onOpenSettings = {},
            onAllowAccess = {},
            onOpenContact = {},
            onCall = {},
            onOpenDndSettings = {},
        )

    @Composable
    private fun Contacts(query: String = "", onToggleStar: (Contact) -> Unit = {}) =
        ContactsScreen(
            contacts = contacts,
            hasAccess = true,
            query = query,
            onQueryChange = {},
            onOpenSettings = {},
            onAllowAccess = {},
            onOpenContact = {},
            onToggleStar = onToggleStar,
        )

    @Test
    fun favoritesStarredDndLight() = render("favorites_dnd-starred_light", dark = false) {
        WithTabs(Tab.FAVORITES) { Favorites(DndCalls.STARRED) }
    }

    @Test
    fun favoritesNoCallsDndDark() = render("favorites_dnd-none_dark", dark = true) {
        WithTabs(Tab.FAVORITES) { Favorites(DndCalls.NONE) }
    }

    @Test
    fun favoritesLargestFont() = render("favorites_dnd-contacts_light_font200", dark = false, fontScale = 2f) {
        WithTabs(Tab.FAVORITES) { Favorites(DndCalls.CONTACTS) }
    }

    @Test
    fun favoritesEmptyDark() = render("favorites_empty_dark", dark = true) {
        WithTabs(Tab.FAVORITES) { Favorites(DndCalls.STARRED, list = contacts.map { it.copy(starred = false) }) }
    }

    @Test
    fun favoritesNoAccessLight() = render("favorites_no-access_light", dark = false) {
        WithTabs(Tab.FAVORITES) { Favorites(DndCalls.UNKNOWN, list = emptyList(), hasAccess = false) }
    }

    @Test
    fun contactsLight() = render("contacts_light", dark = false) { WithTabs(Tab.CONTACTS) { Contacts() } }

    @Test
    fun contactsDarkLargestFont() = render("contacts_dark_font200", dark = true, fontScale = 2f) {
        WithTabs(Tab.CONTACTS) { Contacts() }
    }

    @Test
    fun contactsSearchLight() = render("contacts_search_light", dark = false) {
        WithTabs(Tab.CONTACTS) { Contacts(query = "pat") }
    }

    @Test
    fun contactStarredDark() = render("contact_starred_dark", dark = true) {
        ContactDetailScreen(contacts[1], DndCalls.STARRED, onBack = {}, onToggleStar = {}, onCall = {})
    }

    @Test
    fun contactNotStarredLargestFont() = render("contact_not-starred_light_font200", dark = false, fontScale = 2f) {
        ContactDetailScreen(contacts[2], DndCalls.STARRED, onBack = {}, onToggleStar = {}, onCall = {})
    }

    @Test
    fun favoritesShowOnlyStarredContacts() {
        composeRule.setContent { DialerTheme(darkTheme = false) { Favorites(DndCalls.STARRED) } }
        composeRule.onNodeWithText("Mom").assertIsDisplayed()
        composeRule.onNodeWithText("Alex Rivera").assertIsDisplayed()
        composeRule.onNodeWithText("Pharmacy").assertDoesNotExist()
    }

    @Test
    fun tappingTheStarTogglesThatContact() {
        val toggled = mutableListOf<Long>()
        composeRule.setContent { DialerTheme(darkTheme = false) { Contacts(onToggleStar = { toggled += it.id }) } }
        composeRule.onNodeWithContentDescription("Add Pharmacy to favorites").performClick()
        composeRule.onNodeWithContentDescription("Remove Mom from favorites").performClick()
        assertEquals(listOf(4L, 1L), toggled)
    }

    @Test
    fun callingFromFavoritesUsesTheirNumber() {
        val called = mutableListOf<String>()
        composeRule.setContent {
            DialerTheme(darkTheme = false) {
                FavoritesScreen(
                    contacts = contacts,
                    hasAccess = true,
                    dnd = DndCalls.STARRED,
                    onOpenSettings = {},
                    onAllowAccess = {},
                    onOpenContact = {},
                    onCall = { called += it },
                    onOpenDndSettings = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("Call Mom").performClick()
        assertEquals(listOf("5550197731"), called)
    }
}
