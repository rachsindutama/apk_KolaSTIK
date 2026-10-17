package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = CharcoalPrimary,
    onPrimary = CharcoalOnPrimary,
    primaryContainer = CharcoalPrimaryContainer,
    onPrimaryContainer = CharcoalOnPrimaryContainer,
    secondary = CharcoalSecondary,
    onSecondary = CharcoalOnSecondary,
    secondaryContainer = CharcoalSecondaryContainer,
    onSecondaryContainer = CharcoalOnSecondaryContainer,
    tertiary = CharcoalTertiary,
    onTertiary = CharcoalOnTertiary,
    tertiaryContainer = CharcoalTertiaryContainer,
    onTertiaryContainer = CharcoalOnTertiaryContainer,
    background = CharcoalBackground,
    onBackground = CharcoalOnSurface,
    surface = CharcoalSurface,
    onSurface = CharcoalOnSurface,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = CharcoalOnSurfaceVariant,
    surfaceContainer = CharcoalSurfaceContainer,
    surfaceContainerHigh = CharcoalSurfaceContainerHigh,
    surfaceContainerHighest = CharcoalSurfaceContainerHighest,
    surfaceContainerLow = CharcoalSurfaceContainerLow,
    surfaceContainerLowest = CharcoalSurfaceContainerLowest,
    outline = CharcoalOutline,
    outlineVariant = Color(0xFF444444),
    error = CharcoalError,
    onError = CharcoalOnError
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}

