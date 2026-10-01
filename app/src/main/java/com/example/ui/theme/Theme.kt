package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = OcaGreenLight,
    onPrimary = Color(0xFF003822),
    primaryContainer = OcaGreenDark,
    onPrimaryContainer = OcaGreenContainer,
    secondary = Color(0xFF82B1FF),
    onSecondary = Color(0xFF002244),
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = OcaBlueContainer,
    tertiary = Color(0xFFFBBF24),
    background = OcaDarkBackground,
    surface = OcaDarkSurface,
    surfaceVariant = OcaDarkSurfaceVariant,
    onBackground = OcaDarkTextPrimary,
    onSurface = OcaDarkTextPrimary,
    onSurfaceVariant = OcaDarkTextSecondary,
    outline = OcaDarkOutline,
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF7F1D1D)
)

private val LightColorScheme = lightColorScheme(
    primary = OcaGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = OcaGreenContainer,
    onPrimaryContainer = OcaOnGreenContainer,
    secondary = OcaNavySecondary,
    onSecondary = Color.White,
    secondaryContainer = OcaBlueContainer,
    onSecondaryContainer = OcaOnBlueContainer,
    tertiary = OcaAmberTertiary,
    tertiaryContainer = OcaAmberContainer,
    background = OcaBackground,
    surface = OcaSurface,
    surfaceVariant = OcaSurfaceVariant,
    onBackground = OcaTextPrimary,
    onSurface = OcaTextPrimary,
    onSurfaceVariant = OcaTextSecondary,
    outline = OcaOutline,
    outlineVariant = OcaOutlineVariant,
    error = OcaError,
    errorContainer = OcaErrorContainer
)

@Composable
fun OcaVenteDzTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve brand identity by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Keep backward compatibility alias if needed
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    OcaVenteDzTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
