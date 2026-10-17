package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = CharcoalPrimary,
    secondary = CharcoalSecondary,
    tertiary = CharcoalTertiary,
    background = CharcoalBackground,
    onBackground = CharcoalOnSurface,
    surface = CharcoalSurface,
    onSurface = CharcoalOnSurface,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = CharcoalOnSurfaceVariant,
    outline = CharcoalOutline,
    error = CharcoalError
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

