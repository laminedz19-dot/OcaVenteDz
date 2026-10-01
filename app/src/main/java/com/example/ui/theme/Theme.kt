package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF4ADE80),
  onPrimary = Color(0xFF00391F),
  primaryContainer = EmeraldDark,
  onPrimaryContainer = Color(0xFFA7F3D0),
  secondary = AmberAccent,
  onSecondary = Color(0xFF451A03),
  background = DarkBackground,
  surface = DarkSurface,
  surfaceVariant = DarkSurfaceVariant,
  onBackground = Color(0xFFF1F5F9),
  onSurface = Color(0xFFF1F5F9)
)

private val LightColorScheme = lightColorScheme(
  primary = EmeraldPrimary,
  onPrimary = Color.White,
  primaryContainer = EmeraldLight,
  onPrimaryContainer = EmeraldDark,
  secondary = AmberAccent,
  onSecondary = Color.White,
  secondaryContainer = AmberLight,
  onSecondaryContainer = AmberDark,
  background = SlateLight,
  surface = Color.White,
  surfaceVariant = Color(0xFFF1F5F9),
  onBackground = SlateDark,
  onSurface = SlateDark
)

@Composable
fun OcaventeTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep branded emerald colors consistent
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
