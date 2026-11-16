package com.ultravoice.recorder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import com.ultravoice.recorder.data.ThemeMode

@Immutable
data class UvPalette(
    val bgTop: Color,
    val bgBottom: Color,
    val surface: Color,
    val surfaceHi: Color,
    val accent: Color,
    val danger: Color,
    val muted: Color,
    val onBg: Color,
    val buttonLight: Color,
    val onButtonLight: Color,
)

val DarkPalette = UvPalette(
    bgTop = Color(0xFF111A2E),
    bgBottom = Color(0xFF1D2A47),
    surface = Color(0xFF1B2640),
    surfaceHi = Color(0xFF273558),
    accent = Color(0xFF3D5AFE),
    danger = Color(0xFFD9686A),
    muted = Color(0xFF8C98B8),
    onBg = Color(0xFFF2F4FA),
    buttonLight = Color.White,
    onButtonLight = Color(0xFF111A2E),
)

val LightPalette = UvPalette(
    bgTop = Color(0xFFF5F7FC),
    bgBottom = Color(0xFFE3E9F6),
    surface = Color(0xFFFFFFFF),
    surfaceHi = Color(0xFFDCE3F4),
    accent = Color(0xFF2F46E0),
    danger = Color(0xFFC0393B),
    muted = Color(0xFF5E6A8A),
    onBg = Color(0xFF111A2E),
    buttonLight = Color(0xFF111A2E),
    onButtonLight = Color.White,
)

val LocalUv = staticCompositionLocalOf { DarkPalette }

object Uv {
    val palette: UvPalette
        @Composable @ReadOnlyComposable get() = LocalUv.current
}

@Composable
fun isDarkFor(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun UltraTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = Color.White,
            background = p.bgTop, onBackground = p.onBg,
            surface = p.surface, onSurface = p.onBg,
            surfaceVariant = p.surfaceHi, onSurfaceVariant = p.muted,
            error = p.danger,
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = Color.White,
            background = p.bgTop, onBackground = p.onBg,
            surface = p.surface, onSurface = p.onBg,
            surfaceVariant = p.surfaceHi, onSurfaceVariant = p.muted,
            error = p.danger,
        )
    }
    CompositionLocalProvider(LocalUv provides p, LocalContentColor provides p.onBg) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
