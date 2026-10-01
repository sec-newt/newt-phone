package io.github.secnewt.dialer.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import io.github.secnewt.dialer.SpamProtectionScreen
import io.github.secnewt.dialer.ui.theme.DialerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Draws each screen in light and dark themes and at the largest font size,
 * saves a screenshot for review, and fails on accessibility problems
 * (low contrast, small touch targets, missing labels).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
@OptIn(ExperimentalRoborazziApi::class)
class SpamProtectionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(name: String, roleHeld: Boolean, dark: Boolean, fontScale: Float = 1f) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DialerTheme(darkTheme = dark) {
                    SpamProtectionScreen(roleHeld = roleHeld, onEnable = {})
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

    @Test
    fun offLight() = render("spam-protection_off_light", roleHeld = false, dark = false)

    @Test
    fun offDark() = render("spam-protection_off_dark", roleHeld = false, dark = true)

    @Test
    fun onLight() = render("spam-protection_on_light", roleHeld = true, dark = false)

    @Test
    fun offLargestFont() =
        render("spam-protection_off_light_font200", roleHeld = false, dark = false, fontScale = 2f)

    @Test
    fun onDarkLargestFont() =
        render("spam-protection_on_dark_font200", roleHeld = true, dark = true, fontScale = 2f)
}
