package io.github.secnewt.dialer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult
import com.google.android.apps.common.testing.accessibility.framework.checks.TextContrastCheck
import io.github.secnewt.test.assertTextAtLeast
import io.github.secnewt.ui.NewtLook
import io.github.secnewt.ui.NewtLookTheme
import io.github.secnewt.ui.NewtReadingText
import io.github.secnewt.ui.NewtTheme
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

/**
 * A screenshot plus accessibility checks. [redesigned] screens (the UI refresh) show in reading
 * text, as in the app, and fail on any text under 16sp at the normal font size.
 */
@OptIn(ExperimentalRoborazziApi::class)
fun ComposeContentTestRule.renderScreen(
    name: String,
    dark: Boolean,
    fontScale: Float = 1f,
    terminal: Boolean = false,
    redesigned: Boolean = true,
    contrastFalseAlarm: String? = null,
    content: @Composable () -> Unit,
) {
    setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            NewtTheme(darkTheme = dark) {
                NewtLookTheme(if (terminal) NewtLook.TERMINAL else NewtLook.CLEAN, host = "phone") {
                    if (redesigned) NewtReadingText(content) else content()
                }
            }
        }
    }
    onRoot().captureRoboImage("build/screenshots/$name.png")
    onRoot().checkRoboAccessibility(
        RoborazziATFAccessibilityCheckOptions(
            checker = RoborazziATFAccessibilityChecker(
                checks = AccessibilityCheckPreset.getAccessibilityHierarchyChecksForPreset(AccessibilityCheckPreset.LATEST),
                suppressions = contrastFalseAlarmOn(contrastFalseAlarm),
            ),
            failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Warning,
        )
    )
    if (redesigned && fontScale == 1f) assertTextAtLeast(16f)
}

/** A redesigned screen in reading text, for behavior tests. */
fun ComposeContentTestRule.showScreen(content: @Composable () -> Unit) = setContent {
    NewtTheme(darkTheme = true) { NewtReadingText(content) }
}

/**
 * Ignores the text-contrast check for one label only. The checker estimates colors from its
 * own picture and can miss white text on the blue-to-purple fill: it reported "Watch" as 1.01:1
 * (purple on purple) while the screenshot's pixels are white (255,255,255) on #1A43A3–#5E33B0,
 * about 7.6:1. The library's TonesTest proves text on the fill is 7:1 or better.
 */
private fun contrastFalseAlarmOn(label: String?): Matcher<in AccessibilityViewCheckResult> =
    object : TypeSafeMatcher<AccessibilityViewCheckResult>() {
        override fun matchesSafely(result: AccessibilityViewCheckResult): Boolean =
            label != null &&
                result.sourceCheckClass == TextContrastCheck::class.java &&
                result.element?.text?.toString() == label

        override fun describeTo(description: Description) {
            description.appendText("contrast false alarm on \"$label\"")
        }
    }
