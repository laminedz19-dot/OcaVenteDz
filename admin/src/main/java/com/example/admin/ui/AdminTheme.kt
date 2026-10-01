package com.example.admin.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AdminDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF59E0B),
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF78350F),
    secondary = Color(0xFF008751),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

private val AdminLightColorScheme = lightColorScheme(
    primary = Color(0xFF0F172A), // Deep Slate Navy for Authority/Admin
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF008751), // Emerald
    secondaryContainer = Color(0xFFE8F5E9),
    tertiary = Color(0xFFF59E0B), // Amber Accent
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

@Composable
fun OcaventeAdminTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AdminDarkColorScheme else AdminLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
