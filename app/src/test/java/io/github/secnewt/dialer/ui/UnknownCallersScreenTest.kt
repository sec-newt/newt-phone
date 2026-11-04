package io.github.secnewt.dialer.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import io.github.secnewt.dialer.screening.CallAction
import io.github.secnewt.dialer.screening.ProtectionLevel
import io.github.secnewt.dialer.screening.SpamSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Unknown callers page: protection level, what happens to each kind of caller, who gets through. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class UnknownCallersScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private companion object {
        const val NOW = 1_792_000_000_000L
        const val HOUR = 3_600_000L
    }

    private val custom = SpamSettings(myNumber = "5550142290")
        .withLevel(ProtectionLevel.STRICT)
        .withHiddenNumbers(CallAction.RING)

    private val changes = mutableListOf<SpamSettings>()

    private fun show(settings: SpamSettings = SpamSettings(), onBack: () -> Unit = {}) = composeRule.showScreen {
        UnknownCallersScreen(settings, onSettingsChange = { changes += it }, onBack = onBack, nowMillis = NOW)
    }

    private fun scrollTo(text: String) = composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))

    // Pictures

    @Test
    fun unknownCallersDark() = composeRule.renderScreen("unknown-callers_dark", dark = true) {
        UnknownCallersScreen(SpamSettings(observeOnly = false), onSettingsChange = {}, onBack = {}, nowMillis = NOW)
    }

    @Test
    fun unknownCallersLight() = composeRule.renderScreen("unknown-callers_light", dark = false) {
        UnknownCallersScreen(SpamSettings().withNewNumbers(CallAction.RING), onSettingsChange = {}, onBack = {}, nowMillis = NOW)
    }

    @Test
    fun unknownCallersLargestFont() = composeRule.renderScreen("unknown-callers_dark_font200", dark = true, fontScale = 2f) {
        UnknownCallersScreen(custom, onSettingsChange = {}, onBack = {}, nowMillis = NOW)
    }

    @Test
    fun unknownCallersTerminalDark() = composeRule.renderScreen("terminal_unknown-callers_dark", dark = true, terminal = true) {
        UnknownCallersScreen(SpamSettings(), onSettingsChange = {}, onBack = {}, nowMillis = NOW)
    }

    @Test
    fun letThroughDark() = composeRule.renderScreen("unknown-callers_let-through_dark", dark = true, scrollTo = "Expecting a call") {
        UnknownCallersScreen(SpamSettings(observeOnly = false, expectingUntil = NOW + 3 * HOUR), onSettingsChange = {}, onBack = {}, nowMillis = NOW)
    }

    @Test
    fun letThroughLargestFont() = composeRule.renderScreen(
        "unknown-callers_let-through_light_font200", dark = false, fontScale = 2f, scrollTo = "Expecting a call",
    ) { UnknownCallersScreen(custom, onSettingsChange = {}, onBack = {}, nowMillis = NOW) }

    // Behavior

    @Test
    fun levelChoiceAppliesPreset() {
        show()
        composeRule.onNodeWithText("Strict").performClick()
        assertEquals(SpamSettings().withLevel(ProtectionLevel.STRICT), changes.single())
    }

    @Test
    fun customLevelSelectsNothing() {
        show(custom)
        val levelButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        for (name in listOf("Off", "Balanced", "Strict")) composeRule.onNode(hasText(name) and levelButton).assertIsNotSelected()
        composeRule.onNodeWithText("Custom: you changed the rules below by hand.").assertIsDisplayed()
    }

    @Test
    fun presetLevelIsSelected() {
        show()
        composeRule.onNodeWithText("Balanced").assertIsSelected()
    }

    @Test
    fun chipsShowAction() {
        show(SpamSettings().withNewNumbers(CallAction.RING))
        composeRule.onNode(hasText("New numbers") and hasText("Ring")).assertExists()
        composeRule.onNode(hasText("Hidden numbers") and hasText("Silence")).assertExists()
    }

    @Test
    fun theChoiceSaysWhatTheRowMeans() {
        show()
        composeRule.onNodeWithText("New numbers").performClick()
        composeRule.onNodeWithText("like during a job search", substring = true).assertIsDisplayed()
    }

    @Test
    fun newNumbersCanBeSetToRing() {
        show()
        composeRule.onNodeWithText("New numbers").performClick()
        composeRule.onNodeWithText("Ring like any other call").performClick()
        assertEquals(CallAction.RING, changes.single().newNumbers)
        assertEquals(ProtectionLevel.CUSTOM, changes.single().level)
    }

    @Test
    fun callBacksCanBeTurnedOff() {
        show()
        scrollTo("Call-backs")
        composeRule.onNodeWithText("Call-backs").performClick()
        assertEquals(false, changes.single().ringAfterICall)
    }

    @Test
    fun expectingACallSetsATimeAndTurnsOff() {
        show()
        scrollTo("Expecting a call")
        composeRule.onNodeWithText("Expecting a call").performClick()
        composeRule.onNodeWithText("For 3 hours").performClick()
        assertEquals(NOW + 3 * HOUR, changes.last().expectingUntil)
    }

    @Test
    fun nineOneOneIsLocked() {
        show()
        scrollTo("Expecting a call")
        composeRule.onNodeWithContentDescription("After 911, Always. Every call rings for an hour after you call 911.")
            .assertHasNoClickAction()
    }

    @Test
    fun backGoesBack() {
        var back = 0
        show(onBack = { back++ })
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, back)
    }

}
