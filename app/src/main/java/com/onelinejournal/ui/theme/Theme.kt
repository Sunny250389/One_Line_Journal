package com.onelinejournal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

private val DarkColors = darkColorScheme(
    primary = SoftGreen,
    onPrimary = Ink,
    primaryContainer = SageGreen,
    onPrimaryContainer = Sand
)

@Composable
fun OneLineJournalTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
