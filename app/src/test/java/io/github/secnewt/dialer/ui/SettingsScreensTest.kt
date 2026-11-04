package io.github.secnewt.dialer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
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
import io.github.secnewt.dialer.calls.QuietHours
import io.github.secnewt.dialer.calls.RingSettings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.secnewt.dialer.screening.BlockRule
import io.github.secnewt.dialer.screening.CallAction
import io.github.secnewt.dialer.screening.ProtectionLevel
import io.github.secnewt.dialer.screening.SpamSettings
import io.github.secnewt.ui.NewtTheme
import androidx.compose.ui.test.onNodeWithContentDescription
import io.github.secnewt.ui.NewtSettingsCard
import io.github.secnewt.ui.NewtReadingText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import io.github.secnewt.ui.NewtLook
import io.github.secnewt.ui.NewtLookTheme

/** Screenshots and accessibility checks for the settings and block list screens. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
@OptIn(ExperimentalRoborazziApi::class)
class SettingsScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(
        name: String,
        dark: Boolean,
        fontScale: Float = 1f,
        terminal: Boolean = false,
        redesigned: Boolean = true,
        contrastFalseAlarm: String? = null,
        content: @Composable () -> Unit,
    ) = composeRule.renderScreen(name, dark, fontScale, terminal, redesigned, contrastFalseAlarm, content)

    private fun showSettings(content: @Composable () -> Unit) = composeRule.showScreen(content)

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
    fun settingsTerminalDark() = render("terminal_settings_dark", dark = true, terminal = true) { Settings(SpamSettings()) }

    @Test
    fun settingsTerminalLargestFont() = render("terminal_settings_light_font200", dark = false, fontScale = 2f, terminal = true) {
        Settings(SpamSettings())
    }

    @Test
    fun settingsDefaultLight() = render("settings_default_light", dark = false, contrastFalseAlarm = "Watch") {
        Settings(SpamSettings())
    }

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
    fun blockListEmptyLight() = render("block-list_empty_light", dark = false, redesigned = false) {
        BlockListScreen(rules = emptyList(), onAdd = {}, onRemove = {}, onBack = {})
    }

    @Test
    fun blockListDark() = render("block-list_dark", dark = true, redesigned = false) {
        BlockListScreen(rules = rules, onAdd = {}, onRemove = {}, onBack = {})
    }

    @Test
    fun blockListSharedWithAndroidDark() = render("block-list_shared_dark", dark = true, redesigned = false) {
        BlockListScreen(rules = rules, onAdd = {}, onRemove = {}, onBack = {}, sharedWithAndroid = true)
    }

    @Test
    fun blockListLargestFont() = render("block-list_light_font200", dark = false, fontScale = 2f, redesigned = false) {
        BlockListScreen(rules = rules, onAdd = {}, onRemove = {}, onBack = {})
    }

    @Test
    fun settingsNotPhoneAppYetDark() = render("settings_phone-app-off_dark", dark = true) {
        SpamSettingsScreen(
            SpamSettings(), blockListSize = 0, onSettingsChange = {}, onOpenBlockList = {}, onBack = {},
            isPhoneApp = false,
        )
    }

    @Test
    fun settingsIsPhoneAppLight() = render("settings_phone-app-on_light", dark = false) {
        SpamSettingsScreen(
            SpamSettings(), blockListSize = 0, onSettingsChange = {}, onOpenBlockList = {}, onBack = {},
            isPhoneApp = true,
        )
    }

    @Test
    fun makePhoneAppButtonAsksForTheRole() {
        var asked = 0
        composeRule.setContent {
            NewtTheme(darkTheme = true) {
                SpamSettingsScreen(
                    SpamSettings(), blockListSize = 0, onSettingsChange = {}, onOpenBlockList = {}, onBack = {},
                    isPhoneApp = false, onMakePhoneApp = { asked++ },
                )
            }
        }
        composeRule.onNodeWithText("Make this my phone app").performClick()
        assertEquals(1, asked)
    }

    @Test
    fun settingsPhoneAppDeniedLargestFont() = render("settings_phone-app-denied_dark_font200", dark = true, fontScale = 2f) {
        SpamSettingsScreen(
            SpamSettings(), blockListSize = 0, onSettingsChange = {}, onOpenBlockList = {}, onBack = {},
            isPhoneApp = false, phoneAppDenied = true,
        )
    }

    @Test
    fun deniedHelpOpensAppInfo() {
        val opened = mutableListOf<SystemSetting>()
        composeRule.setContent {
            NewtTheme(darkTheme = true) {
                SpamSettingsScreen(
                    SpamSettings(), blockListSize = 0, onSettingsChange = {}, onOpenBlockList = {}, onBack = {},
                    isPhoneApp = false, phoneAppDenied = true, onOpenSystemSetting = { opened += it },
                )
            }
        }
        composeRule.onNodeWithText("Open App info").performClick()
        assertEquals(listOf(SystemSetting.APP_INFO), opened)
    }

    @Test
    fun phoneSettingsOpenAndroidScreens() {
        val opened = mutableListOf<SystemSetting>()
        composeRule.setContent {
            NewtTheme(darkTheme = false) {
                SpamSettingsScreen(
                    SpamSettings(), blockListSize = 0, onSettingsChange = {}, onOpenBlockList = {}, onBack = {},
                    announce = AnnounceSettings(mode = AnnounceMode.ALWAYS),
                    onOpenSystemSetting = { opened += it },
                )
            }
        }
        val list = composeRule.onNode(hasScrollAction())
        list.performScrollToNode(hasText("Voice and speed"))
        composeRule.onNodeWithText("Voice and speed").performClick()
        list.performScrollToNode(hasText("Ringtone and volume"))
        composeRule.onNodeWithText("Ringtone and volume").performClick()
        list.performScrollToNode(hasText("Calls and SIM"))
        composeRule.onNodeWithText("Calls and SIM").performClick()
        assertEquals(listOf(SystemSetting.TTS, SystemSetting.SOUND, SystemSetting.CALLS), opened)
    }

    @Test
    fun repeatAnnouncementCanBeTurnedOff() {
        val changes = mutableListOf<Boolean>()
        composeRule.setContent {
            NewtTheme(darkTheme = true) {
                SpamSettingsScreen(
                    SpamSettings(), blockListSize = 0, onSettingsChange = {}, onOpenBlockList = {}, onBack = {},
                    announce = AnnounceSettings(mode = AnnounceMode.ALWAYS),
                    onRepeatChange = { changes += it },
                )
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Keep repeating"))
        composeRule.onNodeWithText("Keep repeating").performClick()
        assertEquals(listOf(false), changes)
    }

    @Test
    fun keypadSoundsCanBeTurnedOff() {
        val changes = mutableListOf<Boolean>()
        composeRule.setContent {
            NewtTheme(darkTheme = true) {
                SpamSettingsScreen(
                    SpamSettings(), blockListSize = 0, onSettingsChange = {}, onOpenBlockList = {}, onBack = {},
                    keypadTones = true, onKeypadTonesChange = { changes += it },
                )
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Keypad sounds"))
        composeRule.onNodeWithText("Keypad sounds").performClick()
        assertEquals(listOf(false), changes)
    }

    @Composable
    private fun RingingOnly(settings: RingSettings, onChange: (RingSettings) -> Unit = {}) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                NewtSettingsCard("Ring on silent") { RingingSection(settings, isPhoneApp = true, onChange = onChange) }
            }
        }
    }

    @Test
    fun ringingQuietHoursDark() = render("settings_ringing_dark", dark = true) {
        RingingOnly(RingSettings(quietHours = QuietHours(enabled = true)))
    }

    @Test
    fun ringingLargestFont() = render("settings_ringing_light_font200", dark = false, fontScale = 2f) {
        RingingOnly(RingSettings(quietHours = QuietHours(enabled = true)))
    }

    @Test
    fun ringingSwitchesAndQuietHoursTimes() {
        val changes = mutableListOf<RingSettings>()
        composeRule.setContent {
            NewtTheme(darkTheme = true) { NewtReadingText { RingingOnly(RingSettings(quietHours = QuietHours(enabled = true))) { changes += it } } }
        }
        composeRule.onNodeWithText("Favorites").performClick()
        composeRule.onNodeWithText("10:00", substring = true).assertExists()
        composeRule.onNodeWithText("Ends").performClick()
        composeRule.onNodeWithText("Quiet hours end").assertExists()
        composeRule.onNodeWithText("Set").performClick()
        assertEquals(false, changes[0].starredRingThrough)
        assertEquals(7 * 60, changes[1].quietHours.endMinutes)
    }

    @Test
    fun announcementIsTheFirstCard() {
        showSettings { Settings(SpamSettings()) }
        composeRule.onNodeWithText("Say who's calling").assertIsDisplayed()
        composeRule.onNodeWithText("Always").assertIsDisplayed()
    }

    @Test
    fun summaryShowsAtTop() {
        showSettings { Settings(SpamSettings()) }
        composeRule.onNodeWithContentDescription(
            "Right now. Favorites and repeat callers ring on silent. Spam is watched, not blocked.",
        ).assertIsDisplayed()
    }

    @Test
    fun watchBlockFlipsObserveOnly() {
        val changes = mutableListOf<SpamSettings>()
        showSettings {
            SpamSettingsScreen(SpamSettings(), blockListSize = 0, onSettingsChange = { changes += it }, onOpenBlockList = {}, onBack = {})
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Block"))
        composeRule.onNodeWithText("Block").performClick()
        assertEquals(false, changes.single().observeOnly)
    }

    @Test
    fun announceChoiceGoesThroughCallback() {
        val modes = mutableListOf<AnnounceMode>()
        showSettings {
            SpamSettingsScreen(
                SpamSettings(), blockListSize = 0, onSettingsChange = {}, onOpenBlockList = {}, onBack = {},
                onAnnounceModeChange = { modes += it },
            )
        }
        composeRule.onNodeWithText("Always").performClick()
        assertEquals(listOf(AnnounceMode.ALWAYS), modes)
    }

    @Test
    fun permissionLineShowsWhileOff() {
        showSettings { Settings(SpamSettings()) }
        composeRule.onNodeWithText(
            "Turning this on asks for Phone, Call log and Contacts access, used only on this phone.",
        ).assertIsDisplayed()
    }

    @Test
    fun unknownCallersRowOpensPage() {
        var opened = 0
        showSettings {
            SpamSettingsScreen(
                SpamSettings(), blockListSize = 0, onSettingsChange = {}, onOpenBlockList = {}, onBack = {},
                onOpenUnknownCallers = { opened++ },
            )
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Unknown callers"))
        composeRule.onNode(hasText("Unknown callers") and hasText("Balanced")).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun customLevelReadsCustom() {
        showSettings { Settings(custom) }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Unknown callers"))
        composeRule.onNode(hasText("Unknown callers") and hasText("Custom")).assertExists()
    }
}
