package com.example.chaturbateclient.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Palette for one app theme. Keeping these as plain values (rather than only
 * MaterialTheme roles) lets the OLED theme stay truly black where it wants to.
 */
data class AppColors(
    val background: Color,
    val card: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val thumbPlaceholder: Color,
    val error: Color,
    val divider: Color,
    val isDark: Boolean
)

enum class AppTheme(val label: String) {
    Oled("OLED Black"),
    LightPlus("Light+"),
    SolarizedLight("Solarized Light");

    companion object {
        fun fromLabel(label: String?): AppTheme =
            entries.firstOrNull { it.label == label } ?: Oled
    }
}

private val OledColors = AppColors(
    background = Color.Black,
    card = Color(0xFF0A0A0A),
    surface = Color(0xFF080808),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFF9A9A9A),
    accent = Color(0xFFD8B4FE),
    thumbPlaceholder = Color(0xFF151515),
    error = Color(0xFFFF8A80),
    divider = Color(0xFF1A1A1A),
    isDark = true
)

// Light+ : plain, high-contrast light theme.
private val LightPlusColors = AppColors(
    background = Color(0xFFFFFFFF),
    card = Color(0xFFF2F2F2),
    surface = Color(0xFFF7F7F7),
    textPrimary = Color(0xFF111111),
    textSecondary = Color(0xFF5A5A5A),
    accent = Color(0xFF7C3AED),
    thumbPlaceholder = Color(0xFFE4E4E4),
    error = Color(0xFFB3261E),
    divider = Color(0xFFE0E0E0),
    isDark = false
)

// Solarized Light : Ethan Schoonover's base3/base00 palette.
private val SolarizedLightColors = AppColors(
    background = Color(0xFFFDF6E3),
    card = Color(0xFFEEE8D5),
    surface = Color(0xFFF5EFDC),
    textPrimary = Color(0xFF073642),
    textSecondary = Color(0xFF586E75),
    accent = Color(0xFF268BD2),
    thumbPlaceholder = Color(0xFFE7DFC8),
    error = Color(0xFFDC322F),
    divider = Color(0xFFDDD6C1),
    isDark = false
)

val LocalAppColors = staticCompositionLocalOf { OledColors }

private fun AppColors.toMaterialScheme() = if (isDark) {
    darkColorScheme(
        background = background,
        surface = surface,
        onBackground = textPrimary,
        onSurface = textPrimary,
        primary = accent,
        error = error
    )
} else {
    lightColorScheme(
        background = background,
        surface = surface,
        onBackground = textPrimary,
        onSurface = textPrimary,
        primary = accent,
        error = error
    )
}

@Composable
fun ClientTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val colors = when (theme) {
        AppTheme.Oled -> OledColors
        AppTheme.LightPlus -> LightPlusColors
        AppTheme.SolarizedLight -> SolarizedLightColors
    }
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = colors.toMaterialScheme(), content = content)
    }
}
