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
import io.github.secnewt.dialer.voicemail.ConfigSource
import io.github.secnewt.dialer.voicemail.StatusLine
import io.github.secnewt.dialer.voicemail.StatusReply
import io.github.secnewt.dialer.voicemail.VvmConfig
import io.github.secnewt.dialer.voicemail.VvmText
import io.github.secnewt.dialer.voicemail.VvmType
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

    private fun render(name: String, dark: Boolean, fontScale: Float = 1f, terminal: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                NewtTheme(darkTheme = dark) {
                    NewtLookTheme(if (terminal) NewtLook.TERMINAL else NewtLook.CLEAN, host = "phone") { content() }
                }
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
    private fun Settings(settings: SpamSettings, announce: AnnounceSettings = AnnounceSettings(), nowMillis: Long = NOW) =
        SpamSettingsScreen(
            settings,
            blockListSize = 2,
            onSettingsChange = {},
            onOpenBlockList = {},
            onBack = {},
            announce = announce,
            nowMillis = nowMillis,
        )

    @Test
    fun settingsTerminalDark() = render("terminal_settings_dark", dark = true, terminal = true) { Settings(SpamSettings()) }

    @Test
    fun settingsTerminalLargestFont() = render("terminal_settings_light_font200", dark = false, fontScale = 2f, terminal = true) {
        Settings(SpamSettings())
    }

    @Test
    fun settingsDefaultLight() = render("settings_default_light", dark = false) { Settings(SpamSettings()) }

    @Test
    fun settingsCustomDark() = render("settings_custom_dark", dark = true) { Settings(custom.copy(observeOnly = false)) }

    @Test
    fun settingsNewNumbersRingDark() = render("settings_new-numbers-ring_dark", dark = true) {
        Settings(SpamSettings(observeOnly = false).withNewNumbers(CallAction.RING))
    }

    private companion object {
        const val NOW = 1_792_000_000_000L
        const val HOUR = 3_600_000L
    }

    @Test
    fun letThroughDark() = renderScrolledTo(
        "settings_let-through_dark", "After you call 911, every call rings for an hour so they can call you back. This is always on.",
        dark = true, fontScale = 1f, SpamSettings(observeOnly = false, expectingUntil = NOW + 3 * HOUR),
    )

    @Test
    fun letThroughLargestFont() = renderScrolledTo(
        "settings_let-through_light_font200", "Expecting a call", dark = false, fontScale = 2f, SpamSettings(observeOnly = false),
    )

    @Test
    fun expectingACallSetsATimeAndTurnsOff() {
        val changes = mutableListOf<SpamSettings>()
        composeRule.setContent {
            NewtTheme(darkTheme = true) {
                SpamSettingsScreen(
                    SpamSettings(), blockListSize = 0, onSettingsChange = { changes += it }, onOpenBlockList = {}, onBack = {},
                    nowMillis = NOW,
                )
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Expecting a call"))
        composeRule.onNodeWithText("Expecting a call").performClick()
        composeRule.onNodeWithText("For 3 hours").performClick()
        assertEquals(NOW + 3 * HOUR, changes.last().expectingUntil)
    }

    @Test
    fun callBacksCanBeTurnedOff() {
        val changes = mutableListOf<SpamSettings>()
        composeRule.setContent {
            NewtTheme(darkTheme = true) {
                SpamSettingsScreen(SpamSettings(), blockListSize = 0, onSettingsChange = { changes += it }, onOpenBlockList = {}, onBack = {})
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Call-backs after I call"))
        composeRule.onNodeWithText("Call-backs after I call").performClick()
        assertEquals(false, changes.single().ringAfterICall)
    }

    @Test
    fun theChoiceSaysWhatTheRowMeans() {
        composeRule.setContent {
            NewtTheme(darkTheme = true) {
                SpamSettingsScreen(SpamSettings(), blockListSize = 0, onSettingsChange = {}, onOpenBlockList = {}, onBack = {})
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("New numbers"))
        composeRule.onNodeWithText("New numbers").performClick()
        composeRule.onNodeWithText("like during a job search", substring = true).assertIsDisplayed()
    }

    // The unknown-callers rows sit below the first screenful, so scroll to them first.
    private fun renderScrolledTo(name: String, text: String, dark: Boolean, fontScale: Float, settings: SpamSettings) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                NewtTheme(darkTheme = dark) { NewtLookTheme(NewtLook.CLEAN, host = "phone") { Settings(settings) } }
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
        composeRule.onRoot().captureRoboImage("build/screenshots/$name.png")
        composeRule.onRoot().checkRoboAccessibility(
            RoborazziATFAccessibilityCheckOptions(failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Warning),
        )
    }

    @Test
    fun unknownCallersRowsDark() =
        renderScrolledTo("settings_unknown-callers_dark", "Copycat numbers", dark = true, fontScale = 1f, SpamSettings(observeOnly = false))

    @Test
    fun unknownCallersRowsLargestFont() = renderScrolledTo(
        "settings_unknown-callers_light_font200", "New numbers", dark = false, fontScale = 2f,
        SpamSettings(observeOnly = false).withNewNumbers(CallAction.RING),
    )

    @Test
    fun newNumbersCanBeSetToRing() {
        val changes = mutableListOf<SpamSettings>()
        composeRule.setContent {
            NewtTheme(darkTheme = true) {
                SpamSettingsScreen(SpamSettings(), blockListSize = 0, onSettingsChange = { changes += it }, onOpenBlockList = {}, onBack = {})
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("New numbers"))
        composeRule.onNodeWithText("New numbers").performClick()
        composeRule.onNodeWithText("Ring like any other call").performClick()
        assertEquals(CallAction.RING, changes.single().newNumbers)
        assertEquals(ProtectionLevel.CUSTOM, changes.single().level)
    }

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
    fun blockListSharedWithAndroidDark() = render("block-list_shared_dark", dark = true) {
        BlockListScreen(rules = rules, onAdd = {}, onRemove = {}, onBack = {}, sharedWithAndroid = true)
    }

    @Test
    fun blockListLargestFont() = render("block-list_light_font200", dark = false, fontScale = 2f) {
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
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Repeat until answered"))
        composeRule.onNodeWithText("Repeat until answered").performClick()
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
            Column(Modifier.padding(16.dp)) { RingingSection(settings, isPhoneApp = true, onChange = onChange) }
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
            NewtTheme(darkTheme = true) { RingingOnly(RingSettings(quietHours = QuietHours(enabled = true))) { changes += it } }
        }
        composeRule.onNodeWithText("Starred contacts ring on silent").performClick()
        composeRule.onNodeWithText("From 10:00", substring = true).assertExists()
        composeRule.onNodeWithText("To 7:00", substring = true).performClick()
        composeRule.onNodeWithText("Quiet hours end").assertExists()
        composeRule.onNodeWithText("Set").performClick()
        assertEquals(false, changes[0].starredRingThrough)
        assertEquals(7 * 60, changes[1].quietHours.endMinutes)
    }

    @Test
    fun announcementIsTheFirstSection() {
        composeRule.setContent { NewtTheme(darkTheme = false) { Settings(SpamSettings()) } }
        composeRule.onNodeWithText("Caller announcement").assertIsDisplayed()
        composeRule.onNodeWithText("Announce").assertIsDisplayed()
    }

    private val voicemailSteps = listOf(
        StatusLine(NOW, VvmText.config(VvmConfig(VvmType.CVVM, "122", 1808, false, ConfigSource.TMOBILE_DEFAULTS))),
        StatusLine(NOW, VvmText.STATUS_SENT),
        StatusLine(NOW + 4_000, VvmText.replied(StatusReply.from(mapOf("st" to "R", "rc" to "0")))),
        StatusLine(NOW + 4_000, VvmText.CONNECTING),
        StatusLine(NOW + 6_000, VvmText.loggedIn(3)),
    )

    @Composable
    private fun Voicemail(enabled: Boolean, lines: List<StatusLine> = voicemailSteps, onChange: (Boolean) -> Unit = {}) =
        Surface {
            Column(Modifier.padding(16.dp)) { VoicemailSection(enabled, lines, nowMillis = NOW + 10_000, onEnabledChange = onChange) }
        }

    @Test
    fun voicemailOffDark() = render("voicemail_off_dark", dark = true) { Voicemail(enabled = false) }

    @Test
    fun voicemailStatusDark() = render("voicemail_on-status_dark", dark = true) { Voicemail(enabled = true) }

    @Test
    fun voicemailStatusLight() = render("voicemail_on-status_light", dark = false) { Voicemail(enabled = true) }

    @Test
    fun voicemailStatusLargestFont() = render("voicemail_on-status_light_font200", dark = false, fontScale = 2f) { Voicemail(enabled = true) }

    @Test
    fun voicemailSwitchTurnsOn() {
        val changes = mutableListOf<Boolean>()
        composeRule.setContent { NewtTheme(darkTheme = true) { Voicemail(enabled = false) { changes += it } } }
        composeRule.onNodeWithText("Visual voicemail").performClick()
        assertEquals(listOf(true), changes)
    }

    @Test
    fun voicemailSaysWhenTingHasNotAnswered() {
        composeRule.setContent {
            NewtTheme(darkTheme = true) {
                VoicemailSection(true, listOf(StatusLine(NOW, VvmText.STATUS_SENT)), nowMillis = NOW + 3 * 60_000, onEnabledChange = {})
            }
        }
        composeRule.onNodeWithText(VvmText.WAITING).assertIsDisplayed()
    }

    @Test
    fun voicemailSectionIsInSettings() {
        composeRule.setContent { NewtTheme(darkTheme = true) { Settings(SpamSettings()) } }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Download your voicemails and read them here. Uses mobile data to reach Ting, nothing else."))
    }
}
