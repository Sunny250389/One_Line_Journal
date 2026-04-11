package com.onelinejournal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = SageGreen,
    onPrimary = Sand,
    primaryContainer = SoftGreen,
    onPrimaryContainer = Ink,
    background = Sand,
    onBackground = Ink,
    surface = Sand,
    onSurface = Ink,
    surfaceContainerHighest = Mist
)

@Composable
fun OneLineJournalTheme(
    accentTheme: AccentTheme = AccentTheme.Green,
    content: @Composable () -> Unit
) {
    val accent = accentTheme.color
    val colors = LightColors.copy(
        primary = accent,
        onPrimary = if (accentTheme == AccentTheme.Monochrome) Color.White else Sand,
        secondary = accent,
        tertiary = accent
    )
    val selectionColors = TextSelectionColors(
        handleColor = accent,
        backgroundColor = accent.copy(alpha = 0.28f)
    )

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            content = content
        )
    }
}
