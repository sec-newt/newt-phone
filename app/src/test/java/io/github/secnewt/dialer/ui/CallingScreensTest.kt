package io.github.secnewt.dialer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import io.github.secnewt.dialer.announce.AnnounceMode
import io.github.secnewt.dialer.calls.CallHistory
import io.github.secnewt.dialer.calls.CallKind
import io.github.secnewt.dialer.calls.LoggedCall
import io.github.secnewt.dialer.contacts.Contact
import io.github.secnewt.dialer.contacts.PhoneEntry
import io.github.secnewt.dialer.screening.CallAction
import io.github.secnewt.dialer.screening.Reason
import io.github.secnewt.dialer.screening.ScreenedCall
import io.github.secnewt.dialer.screening.Verification
import io.github.secnewt.dialer.ui.theme.DialerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneId
import java.time.ZonedDateTime

/** Screenshots, accessibility checks and taps for Recents and the dialpad. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
@OptIn(ExperimentalRoborazziApi::class)
class CallingScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val zone = ZoneId.of("America/Chicago")
    private val now = ZonedDateTime.of(2026, 10, 6, 18, 0, 0, 0, zone)
    private fun ago(hours: Long) = now.minusHours(hours).toInstant().toEpochMilli()

    private val calls = listOf(
        LoggedCall(1, "5550197731", null, CallKind.MISSED, ago(1)),
        LoggedCall(2, "5550123456", "Mom", CallKind.INCOMING, ago(2), durationSeconds = 754),
        LoggedCall(3, "5550123456", "Mom", CallKind.INCOMING, ago(3), durationSeconds = 30),
        LoggedCall(4, "5550142290", "Alex Rivera", CallKind.OUTGOING, ago(26), durationSeconds = 95),
        LoggedCall(5, null, null, CallKind.MISSED, ago(30)),
        LoggedCall(6, "8005550000", null, CallKind.BLOCKED, ago(50)),
    )
    private val screened = listOf(
        ScreenedCall(ago(1), "5550197731", Verification.FAILED, CallAction.SILENCE, Reason.FAILED_VERIFICATION),
        ScreenedCall(ago(50), "8005550000", Verification.NONE, CallAction.BLOCK, Reason.BLOCK_LIST, enforced = true),
    )
    private val contacts = listOf(
        Contact(1, "Mom", starred = true, phones = listOf(PhoneEntry("5550123456", "Mobile"))),
        Contact(2, "Mona's bakery", starred = false, phones = listOf(PhoneEntry("5550129999", "Work"))),
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
    private fun Recents(hasAccess: Boolean = true, roleHeld: Boolean = true, onCall: (String) -> Unit = {}) =
        Scaffold(bottomBar = { DialerTabBar(selected = Tab.RECENTS, onSelect = {}) }) { padding ->
            Box(Modifier.padding(padding)) {
                RecentsScreen(
                    groups = CallHistory.group(calls, zone),
                    hasAccess = hasAccess,
                    screened = screened,
                    roleHeld = roleHeld,
                    observeOnly = true,
                    announceMode = AnnounceMode.ALWAYS,
                    onOpenSettings = {},
                    onOpenSpamProtection = {},
                    onEnableSpamProtection = {},
                    onAllowAccess = {},
                    onCall = onCall,
                    now = now,
                )
            }
        }

    @Composable
    private fun Pad(number: String) = DialpadScreen(number, {}, contacts, onCall = {}, onBack = {})

    @Test
    fun recentsDark() = render("recents_dark", dark = true) { Recents() }

    @Test
    fun recentsLight() = render("recents_light", dark = false) { Recents() }

    @Test
    fun recentsLargestFont() = render("recents_dark_font200", dark = true, fontScale = 2f) { Recents() }

    @Test
    fun recentsNoAccess() = render("recents_no-access_dark", dark = true) { Recents(hasAccess = false, roleHeld = false) }

    @Test
    fun dialpadEmptyDark() = render("dialpad_empty_dark", dark = true) { Pad("") }

    @Test
    fun dialpadTypingDark() = render("dialpad_typing_dark", dark = true) { Pad("555012") }

    @Test
    fun dialpadLargestFont() = render("dialpad_light_font200", dark = false, fontScale = 2f) { Pad("5550197731") }

    @Test
    fun recentsFlagSpamAndCallBack() {
        val called = mutableListOf<String>()
        composeRule.setContent { DialerTheme(darkTheme = true) { Recents(onCall = { called += it }) } }
        composeRule.onNodeWithText("Likely spam").assertIsDisplayed()
        composeRule.onNodeWithText("Incoming · 2 calls", substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Call Mom").performClick()
        assertEquals(listOf("5550123456"), called)
    }

    @Test
    fun dialpadTypesHoldsForPlusDeletesAndCalls() {
        var number by mutableStateOf("")
        val called = mutableListOf<String>()
        composeRule.setContent {
            DialerTheme(darkTheme = true) {
                DialpadScreen(number, { number = it }, contacts, onCall = { called += it }, onBack = {})
            }
        }
        composeRule.onNodeWithContentDescription("0, hold for plus").performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithContentDescription("1, hold for voicemail").performClick()
        composeRule.onNodeWithContentDescription("5, J K L").performClick()
        composeRule.onNodeWithContentDescription("5, J K L").performClick()
        assertEquals("+155", number)
        composeRule.onNodeWithContentDescription("Delete last digit").performClick()
        assertEquals("+15", number)
        composeRule.onNodeWithContentDescription("Call").performClick()
        assertEquals(listOf("+15"), called)
    }

    @Test
    fun holdingOneCallsVoicemail() {
        var voicemail = 0
        composeRule.setContent {
            DialerTheme(darkTheme = true) {
                DialpadScreen("", {}, contacts, onCall = {}, onBack = {}, onVoicemail = { voicemail++ })
            }
        }
        composeRule.onNodeWithContentDescription("1, hold for voicemail").performSemanticsAction(SemanticsActions.OnLongClick)
        assertEquals(1, voicemail)
    }

    @Test
    fun dialpadStartsWithFavorites() {
        composeRule.setContent {
            DialerTheme(darkTheme = true) { DialpadScreen("", {}, contacts, onCall = {}, onBack = {}) }
        }
        composeRule.onNodeWithText("Favorites").assertIsDisplayed()
        composeRule.onNodeWithText("Mom").assertIsDisplayed()
        composeRule.onNodeWithText("Mona's bakery").assertDoesNotExist()
    }

    @Test
    fun dialpadCallsAMatchDirectly() {
        val called = mutableListOf<String>()
        composeRule.setContent {
            DialerTheme(darkTheme = true) { DialpadScreen("555012", {}, contacts, onCall = { called += it }, onBack = {}) }
        }
        composeRule.onNodeWithContentDescription("Call Mona's bakery, (555) 012-9999").performClick()
        assertEquals(listOf("5550129999"), called)
    }

    @Test
    fun dialpadSuggestsMatchingContacts() {
        var number by mutableStateOf("555012")
        composeRule.setContent {
            DialerTheme(darkTheme = true) { DialpadScreen(number, { number = it }, contacts, onCall = {}, onBack = {}) }
        }
        composeRule.onNodeWithText("Mom").assertIsDisplayed()
        composeRule.onNodeWithText("Mona's bakery").performClick()
        assertEquals("5550129999", number)
    }
}
