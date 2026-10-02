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
import io.github.secnewt.dialer.screening.Decision
import io.github.secnewt.dialer.screening.ScreenedCall
import io.github.secnewt.dialer.screening.Verification
import io.github.secnewt.dialer.ui.theme.DialerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneId
import java.time.ZonedDateTime

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

    private val now = ZonedDateTime.of(2026, 10, 1, 18, 0, 0, 0, ZoneId.of("America/Chicago"))

    private fun at(hoursAgo: Long) = now.minusHours(hoursAgo).toInstant().toEpochMilli()

    private val sampleCalls = listOf(
        ScreenedCall(at(1), "5550197731", Verification.FAILED, Decision.ALLOWED),
        ScreenedCall(at(3), "+15550142290", Verification.PASSED, Decision.ALLOWED),
        ScreenedCall(at(20), null, Verification.NONE, Decision.ALLOWED),
        ScreenedCall(at(30), "8005550000", Verification.NONE, Decision.ALLOWED),
    )

    private fun render(
        name: String,
        roleHeld: Boolean,
        calls: List<ScreenedCall>,
        dark: Boolean,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DialerTheme(darkTheme = dark) {
                    SpamProtectionScreen(
                        roleHeld = roleHeld,
                        recentCalls = calls,
                        onEnable = {},
                        now = now,
                    )
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
    fun offLight() = render("spam-protection_off_light", roleHeld = false, calls = emptyList(), dark = false)

    @Test
    fun offDark() = render("spam-protection_off_dark", roleHeld = false, calls = emptyList(), dark = true)

    @Test
    fun onNoCallsLight() =
        render("spam-protection_on_no-calls_light", roleHeld = true, calls = emptyList(), dark = false)

    @Test
    fun onWithCallsLight() =
        render("spam-protection_on_calls_light", roleHeld = true, calls = sampleCalls, dark = false)

    @Test
    fun onWithCallsDark() =
        render("spam-protection_on_calls_dark", roleHeld = true, calls = sampleCalls, dark = true)

    @Test
    fun offLargestFont() = render(
        "spam-protection_off_light_font200", roleHeld = false, calls = emptyList(), dark = false, fontScale = 2f,
    )

    @Test
    fun onWithCallsDarkLargestFont() = render(
        "spam-protection_on_calls_dark_font200", roleHeld = true, calls = sampleCalls, dark = true, fontScale = 2f,
    )
}
