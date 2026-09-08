package com.yashjayswal.dairy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// A warm, personal palette (terracotta + soft plum) instead of Material3's
// generic default purple — this is a journal, it should feel handwritten,
// not like generic corporate software.
private val LightColors = lightColorScheme(
    primary = Color(0xFFB75C4B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBD1),
    onPrimaryContainer = Color(0xFF3B0900),
    secondary = Color(0xFF8C6D5C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF6DED0),
    onSecondaryContainer = Color(0xFF2E1A0E),
    tertiary = Color(0xFF6B5C8C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE6DEFF),
    onTertiaryContainer = Color(0xFF211444),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF231917),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF231917),
    surfaceVariant = Color(0xFFF3DED8),
    onSurfaceVariant = Color(0xFF52443F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB5A0),
    onPrimary = Color(0xFF5F1600),
    primaryContainer = Color(0xFF7D2E1B),
    onPrimaryContainer = Color(0xFFFFDBD1),
    secondary = Color(0xFFDBC1AF),
    onSecondary = Color(0xFF3F2D1F),
    secondaryContainer = Color(0xFF574435),
    onSecondaryContainer = Color(0xFFF6DED0),
    tertiary = Color(0xFFCBC0F7),
    onTertiary = Color(0xFF352969),
    tertiaryContainer = Color(0xFF4C4080),
    onTertiaryContainer = Color(0xFFE6DEFF),
    background = Color(0xFF1D1512),
    onBackground = Color(0xFFEDE0DB),
    surface = Color(0xFF1D1512),
    onSurface = Color(0xFFEDE0DB),
    surfaceVariant = Color(0xFF52443F),
    onSurfaceVariant = Color(0xFFD7C2BA),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun DairyTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (isDark) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
