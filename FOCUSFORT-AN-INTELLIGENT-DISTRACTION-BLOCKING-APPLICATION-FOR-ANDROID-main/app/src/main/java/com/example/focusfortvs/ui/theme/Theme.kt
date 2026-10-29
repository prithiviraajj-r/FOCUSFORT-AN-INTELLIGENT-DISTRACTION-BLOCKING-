package com.example.focusfortvs.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = FocusPrimary,
    onPrimary = FocusOnPrimary,
    secondary = FocusSecondary,
    onSecondary = FocusOnSecondary,
    background = FocusBackground,
    onBackground = FocusOnBackground,
    surface = FocusSurface,
    onSurface = FocusOnSurface,
)

private val DarkColors = darkColorScheme(
    primary = FocusPrimary,
    onPrimary = FocusOnPrimary,
    secondary = FocusSecondary,
    onSecondary = FocusOnSecondary,
)

@Composable
fun FocusFortTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
