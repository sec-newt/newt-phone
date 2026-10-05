package io.github.secnewt.dialer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import io.github.secnewt.dialer.announce.AnnounceMode
import io.github.secnewt.dialer.announce.AnnounceSettings
import io.github.secnewt.dialer.screening.BlockRule
import io.github.secnewt.dialer.screening.CallAction
import io.github.secnewt.dialer.screening.ProtectionLevel
import io.github.secnewt.dialer.screening.SpamSettings
import io.github.secnewt.dialer.ui.theme.DialerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Screenshots and accessibility checks for the settings and block list screens. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
@OptIn(ExperimentalRoborazziApi::class)
class SettingsScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

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

    private val custom = SpamSettings(myNumber = "5550142290")
        .withLevel(ProtectionLevel.STRICT)
        .withHiddenNumbers(CallAction.RING)

    private val rules = listOf(BlockRule.Number("5550197731"), BlockRule.StartsWith("800555"))

    @Composable
    private fun Settings(settings: SpamSettings, announce: AnnounceSettings = AnnounceSettings()) =
        SpamSettingsScreen(
            settings,
            blockListSize = 2,
            onSettingsChange = {},
            onOpenBlockList = {},
            onBack = {},
            announce = announce,
        )

    @Test
    fun settingsDefaultLight() = render("settings_default_light", dark = false) { Settings(SpamSettings()) }

    @Test
    fun settingsCustomDark() = render("settings_custom_dark", dark = true) { Settings(custom.copy(observeOnly = false)) }

    @Test
    fun settingsLargestFont() = render("settings_default_light_font200", dark = false, fontScale = 2f) {
        Settings(SpamSettings())
    }

    @Test
    fun settingsAnnounceOnLight() = render("settings_announce-on_light", dark = false) {
        Settings(SpamSettings(), AnnounceSettings(mode = AnnounceMode.ALWAYS))
    }

    @Test
    fun settingsAnnounceOnDarkLargestFont() = render("settings_announce-on_dark_font200", dark = true, fontScale = 2f) {
        Settings(SpamSettings(), AnnounceSettings(mode = AnnounceMode.HEADPHONES_ONLY))
    }

    @Test
    fun blockListEmptyLight() = render("block-list_empty_light", dark = false) {
        BlockListScreen(rules = emptyList(), onAdd = {}, onRemove = {}, onBack = {})
    }

    @Test
    fun blockListDark() = render("block-list_dark", dark = true) {
        BlockListScreen(rules = rules, onAdd = {}, onRemove = {}, onBack = {})
    }

    @Test
    fun blockListLargestFont() = render("block-list_light_font200", dark = false, fontScale = 2f) {
        BlockListScreen(rules = rules, onAdd = {}, onRemove = {}, onBack = {})
    }

    @Test
    fun announcementIsTheFirstSection() {
        composeRule.setContent { DialerTheme(darkTheme = false) { Settings(SpamSettings()) } }
        composeRule.onNodeWithText("Caller announcement").assertIsDisplayed()
        composeRule.onNodeWithText("Announce").assertIsDisplayed()
    }
}
