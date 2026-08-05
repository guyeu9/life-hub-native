package com.lifehub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = Clay,
    onPrimary = PaperCard,
    primaryContainer = ClayLight,
    onPrimaryContainer = Ink,
    secondary = Sage,
    onSecondary = PaperCard,
    secondaryContainer = SageLight,
    background = PaperBg,
    onBackground = Ink,
    surface = PaperCard,
    onSurface = Ink,
    surfaceVariant = PaperBg,
    onSurfaceVariant = InkSoft,
    outline = Line,
    error = Danger
)

private val DarkScheme = darkColorScheme(
    primary = Clay,
    onPrimary = Ink,
    primaryContainer = Clay,
    secondary = Sage,
    background = Color(0xFF1E1B18),
    onBackground = Color(0xFFE8E4DD),
    surface = Color(0xFF262220),
    onSurface = Color(0xFFE8E4DD),
    surfaceVariant = Color(0xFF332E2A),
    onSurfaceVariant = Color(0xFFB0A9A0),
    outline = Color(0xFF4A443F),
    error = Danger
)

@Composable
fun LifeHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = LifeHubTypography,
        content = content
    )
}
