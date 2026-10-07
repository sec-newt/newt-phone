package io.github.secnewt.dialer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
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
import io.github.secnewt.dialer.calls.AudioRoute
import io.github.secnewt.dialer.calls.AudioState
import io.github.secnewt.dialer.calls.CallPhase
import io.github.secnewt.dialer.calls.LiveCall
import io.github.secnewt.dialer.ui.theme.DialerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Screenshots, accessibility checks and taps for the incoming-call and in-call screens. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
@OptIn(ExperimentalRoborazziApi::class)
class InCallScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val now = 1_800_000_000_000L

    private val mom = LiveCall(
        id = "call-1", number = "5550197731", name = "Mom", phase = CallPhase.ACTIVE,
        connectedAtMillis = now - 125_000, canHold = true,
    )
    private val unknown = LiveCall(id = "call-2", number = "+1 972-555-0142", name = null, phase = CallPhase.RINGING)

    private fun render(name: String, dark: Boolean, fontScale: Float = 1f, content: @Composable () -> Unit) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DialerTheme(darkTheme = dark) { content() }
            }
        }
        snapshot(name)
    }

    private fun snapshot(name: String) {
        composeRule.onRoot().captureRoboImage("build/screenshots/$name.png")
        composeRule.onRoot().checkRoboAccessibility(
            RoborazziATFAccessibilityCheckOptions(
                failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Warning,
            )
        )
    }

    @Composable
    private fun Screen(
        call: LiveCall,
        other: LiveCall? = null,
        audio: AudioState = AudioState(),
        actions: CallActions = CallActions(),
    ) = CallScreen(call = call, other = other, audio = audio, actions = actions, nowMillis = now)

    @Test
    fun incomingFromAContactDark() = render("incall_incoming_contact_dark", dark = true) {
        Screen(mom.copy(phase = CallPhase.RINGING, connectedAtMillis = null))
    }

    @Test
    fun incomingLikelySpamLight() = render("incall_incoming_spam_light", dark = false) {
        Screen(unknown.copy(spamLabel = "Likely spam"))
    }

    @Test
    fun incomingWhileOnACallLargestFont() = render("incall_incoming_waiting_dark_font200", dark = true, fontScale = 2f) {
        Screen(unknown, other = mom)
    }

    @Test
    fun inCallDark() = render("incall_active_dark", dark = true) { Screen(mom) }

    @Test
    fun inCallMutedOnSpeakerLight() = render("incall_muted-speaker_light", dark = false) {
        Screen(mom, audio = AudioState(route = AudioRoute.SPEAKER, muted = true))
    }

    @Test
    fun inCallWithBluetoothAndAHeldCallDark() = render("incall_bluetooth-held_dark", dark = true) {
        val available = setOf(AudioRoute.EARPIECE, AudioRoute.SPEAKER, AudioRoute.BLUETOOTH)
        Screen(
            unknown.copy(phase = CallPhase.ACTIVE, connectedAtMillis = now - 5_000),
            other = mom.copy(phase = CallPhase.ON_HOLD),
            audio = AudioState(route = AudioRoute.BLUETOOTH, available = available),
        )
    }

    @Test
    fun inCallLargestFont() = render("incall_active_light_font200", dark = false, fontScale = 2f) { Screen(mom) }

    @Test
    fun callingDark() = render("incall_dialing_dark", dark = true) {
        Screen(mom.copy(phase = CallPhase.DIALING, connectedAtMillis = null))
    }

    @Test
    fun callEndedDark() = render("incall_ended_dark", dark = true) { Screen(mom.copy(phase = CallPhase.ENDED)) }

    @Test
    fun keypadOpenDark() {
        composeRule.setContent { DialerTheme(darkTheme = true) { Screen(mom) } }
        composeRule.onNodeWithText("Keypad").performClick()
        snapshot("incall_keypad_dark")
    }

    @Test
    fun answerAndDeclineReportTheCall() {
        val answered = mutableListOf<String>()
        val declined = mutableListOf<String>()
        composeRule.setContent {
            DialerTheme(darkTheme = true) {
                Screen(unknown, actions = CallActions(onAnswer = { answered += it }, onDecline = { declined += it }))
            }
        }
        composeRule.onNodeWithText("Answer").performClick()
        composeRule.onNodeWithText("Decline").performClick()
        assertEquals(listOf("call-2"), answered)
        assertEquals(listOf("call-2"), declined)
    }

    @Test
    fun theTimerShowsHowLongTheCallHasGone() {
        composeRule.setContent { DialerTheme(darkTheme = true) { Screen(mom) } }
        composeRule.onNodeWithText("2:05").assertExists()
        composeRule.onNodeWithText("(555) 019-7731").assertExists()
    }

    @Test
    fun muteAndSpeakerAreSwitches() {
        val muted = mutableListOf<Boolean>()
        val routes = mutableListOf<AudioRoute>()
        composeRule.setContent {
            DialerTheme(darkTheme = true) {
                Screen(
                    mom,
                    audio = AudioState(route = AudioRoute.SPEAKER),
                    actions = CallActions(onMute = { muted += it }, onRoute = { routes += it }),
                )
            }
        }
        composeRule.onNodeWithText("Mute").assertIsOff().performClick()
        composeRule.onNodeWithText("Speaker").assertIsOn().performClick()
        assertEquals(listOf(true), muted)
        assertEquals(listOf(AudioRoute.EARPIECE), routes)
    }

    @Test
    fun keypadSendsTonesToTheCall() {
        val tones = mutableListOf<Pair<String, Char>>()
        composeRule.setContent {
            DialerTheme(darkTheme = true) {
                Screen(mom, actions = CallActions(onTone = { id, digit -> tones += id to digit }))
            }
        }
        composeRule.onNodeWithText("Keypad").performClick()
        composeRule.onNodeWithContentDescription("1").performClick()
        composeRule.onNodeWithContentDescription("Pound").performClick()
        assertEquals(listOf("call-1" to '1', "call-1" to '#'), tones)
        composeRule.onNodeWithText("Hide keypad").performClick()
        composeRule.onNodeWithText("Mute").assertExists()
    }

    @Test
    fun endCallHangsUp() {
        val ended = mutableListOf<String>()
        composeRule.setContent {
            DialerTheme(darkTheme = true) { Screen(mom, actions = CallActions(onHangUp = { ended += it })) }
        }
        composeRule.onNodeWithText("End call").performClick()
        assertEquals(listOf("call-1"), ended)
    }

    @Test
    fun swapPicksUpTheHeldCall() {
        var swapped = 0
        composeRule.setContent {
            DialerTheme(darkTheme = true) {
                Screen(
                    unknown.copy(phase = CallPhase.ACTIVE, connectedAtMillis = now),
                    other = mom.copy(phase = CallPhase.ON_HOLD),
                    actions = CallActions(onSwap = { swapped++ }),
                )
            }
        }
        composeRule.onNodeWithText("On hold: Mom").assertExists()
        composeRule.onNodeWithText("Swap").performClick()
        assertEquals(1, swapped)
    }

    @Test
    fun mergeJoinsTheHeldCall() {
        val merged = mutableListOf<String>()
        composeRule.setContent {
            DialerTheme(darkTheme = true) {
                Screen(
                    unknown.copy(phase = CallPhase.ACTIVE, connectedAtMillis = now, canMerge = true),
                    other = mom.copy(phase = CallPhase.ON_HOLD),
                    actions = CallActions(onMerge = { merged += it }),
                )
            }
        }
        composeRule.onNodeWithText("Merge").performClick()
        assertEquals(listOf("call-2"), merged)
    }

    @Test
    fun noMergeButtonWhenTheNetworkCantMerge() {
        composeRule.setContent {
            DialerTheme(darkTheme = true) {
                Screen(
                    unknown.copy(phase = CallPhase.ACTIVE, connectedAtMillis = now),
                    other = mom.copy(phase = CallPhase.ON_HOLD),
                )
            }
        }
        composeRule.onNodeWithText("Merge").assertDoesNotExist()
    }

    @Test
    fun conferenceDark() = render("incall_conference_dark", dark = true) {
        val people = listOf(
            mom.copy(id = "call-3", parentId = "conf"),
            unknown.copy(id = "call-4", phase = CallPhase.ACTIVE, parentId = "conf"),
        )
        Screen(
            LiveCall(
                id = "conf", number = null, name = null, phase = CallPhase.ACTIVE,
                connectedAtMillis = now - 61_000, canHold = true, isConference = true, participants = people,
            )
        )
    }

    @Test
    fun mergeAvailableLargestFont() = render("incall_merge_light_font200", dark = false, fontScale = 2f) {
        Screen(
            unknown.copy(phase = CallPhase.ACTIVE, connectedAtMillis = now - 5_000, canMerge = true),
            other = mom.copy(phase = CallPhase.ON_HOLD),
        )
    }
}
