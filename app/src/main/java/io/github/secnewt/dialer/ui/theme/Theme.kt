package io.github.secnewt.dialer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.secnewt.dialer.R

/**
 * Fonts, all bundled (no downloads):
 * Chakra Petch for screen titles only, Atkinson Hyperlegible (designed for low vision)
 * for names and everything you read, and JetBrains Mono for phone numbers so digits line up.
 */
object DialerFonts {
    val Display = FontFamily(
        Font(R.font.chakra_semibold, FontWeight.SemiBold),
        Font(R.font.chakra_bold, FontWeight.Bold),
    )
    val Body = FontFamily(
        Font(R.font.atkinson_regular, FontWeight.Normal),
        Font(R.font.atkinson_bold, FontWeight.Bold),
    )
    val Mono = FontFamily(
        Font(
            R.font.jetbrains_mono,
            FontWeight.Medium,
            variationSettings = FontVariation.Settings(FontVariation.weight(500)),
        ),
        Font(
            R.font.jetbrains_mono,
            FontWeight.Bold,
            variationSettings = FontVariation.Settings(FontVariation.weight(700)),
        ),
    )
}

// Tokyo Night on true black. Text and accents stay at or above 7:1 against the background;
// neon is used for accents (selected tab, rings, headings), never behind text you have to read.
private val Night = darkColorScheme(
    primary = Color(0xFF85ABF8),
    onPrimary = Color(0xFF0B0C14),
    primaryContainer = Color(0xFF253055),
    onPrimaryContainer = Color(0xFFDCE3FF),
    secondary = Color(0xFFBB9AF7),
    onSecondary = Color(0xFF14091F),
    secondaryContainer = Color(0xFF2E2552),
    onSecondaryContainer = Color(0xFFEDE4FF),
    tertiary = Color(0xFF7DCFFF),
    onTertiary = Color(0xFF041520),
    background = Color(0xFF050608),
    onBackground = Color(0xFFE2E6FF),
    surface = Color(0xFF050608),
    onSurface = Color(0xFFE2E6FF),
    surfaceVariant = Color(0xFF161925),
    onSurfaceVariant = Color(0xFFB4BBDD),
    surfaceContainerLowest = Color(0xFF050608),
    surfaceContainerLow = Color(0xFF0E1018),
    surfaceContainer = Color(0xFF12141E),
    surfaceContainerHigh = Color(0xFF161925),
    surfaceContainerHighest = Color(0xFF1C2030),
    outline = Color(0xFF565F89),
    outlineVariant = Color(0xFF2A2F45),
    error = Color(0xFFFF7A93),
    onError = Color(0xFF1F0610),
)

// Tokyo Night Day, for when the phone is in light mode.
private val Day = lightColorScheme(
    primary = Color(0xFF1A43A3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE3FF),
    onPrimaryContainer = Color(0xFF0B1B4A),
    secondary = Color(0xFF5E33B0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6DCFF),
    onSecondaryContainer = Color(0xFF1E0A45),
    tertiary = Color(0xFF00597A),
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF1A1B26),
    surface = Color.White,
    onSurface = Color(0xFF1A1B26),
    surfaceVariant = Color(0xFFECEEF6),
    onSurfaceVariant = Color(0xFF2F3549),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F7FB),
    surfaceContainer = Color(0xFFF0F2F8),
    surfaceContainerHigh = Color(0xFFECEEF6),
    surfaceContainerHighest = Color(0xFFE4E7F1),
    outline = Color(0xFF6B7194),
    outlineVariant = Color(0xFFCDD1E3),
    error = Color(0xFF94102A),
    onError = Color.White,
)

private val Base = Typography()

private val DialerTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = DialerFonts.Display),
    displayMedium = Base.displayMedium.copy(fontFamily = DialerFonts.Display),
    displaySmall = Base.displaySmall.copy(fontFamily = DialerFonts.Display),
    headlineLarge = Base.headlineLarge.copy(fontFamily = DialerFonts.Display),
    headlineMedium = Base.headlineMedium.copy(fontFamily = DialerFonts.Display),
    headlineSmall = Base.headlineSmall.copy(fontFamily = DialerFonts.Display),
    titleLarge = Base.titleLarge.copy(fontFamily = DialerFonts.Body),
    titleMedium = Base.titleMedium.copy(fontFamily = DialerFonts.Body),
    titleSmall = Base.titleSmall.copy(fontFamily = DialerFonts.Body),
    bodyLarge = Base.bodyLarge.copy(fontFamily = DialerFonts.Body),
    bodyMedium = Base.bodyMedium.copy(fontFamily = DialerFonts.Body),
    bodySmall = Base.bodySmall.copy(fontFamily = DialerFonts.Body),
    labelLarge = Base.labelLarge.copy(fontFamily = DialerFonts.Body),
    labelMedium = Base.labelMedium.copy(fontFamily = DialerFonts.Body),
    labelSmall = Base.labelSmall.copy(fontFamily = DialerFonts.Body),
)

@Composable
fun DialerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) Night else Day,
        typography = DialerTypography,
        content = content,
    )
}
