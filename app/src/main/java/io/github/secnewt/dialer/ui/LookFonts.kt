package io.github.secnewt.dialer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.font.FontFamily
import io.github.secnewt.ui.NewtFonts
import io.github.secnewt.ui.terminalLook

/** Chakra Petch titles in the clean look; JetBrains Mono in the terminal look. */
@Composable
@ReadOnlyComposable
fun displayFont(): FontFamily = if (terminalLook()) NewtFonts.Mono else NewtFonts.Display

/** Atkinson Hyperlegible in the clean look; JetBrains Mono in the terminal look. */
@Composable
@ReadOnlyComposable
fun bodyFont(): FontFamily = if (terminalLook()) NewtFonts.Mono else NewtFonts.Body
