package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = TvNeonCyan,
    secondary = TvNeonPurple,
    tertiary = Pink80,
    background = TvBgDark,
    surface = TvBgCard,
    onBackground = TvTextPrimary,
    onSurface = TvTextPrimary,
    surfaceVariant = TvBgCardFocused
  )

private val LightColorScheme = DarkColorScheme // Always force dark theme on TV for cinema look

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme
  dynamicColor: Boolean = false, // Disable dynamic colors to keep curated neon vibe
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
