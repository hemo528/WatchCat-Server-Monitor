package com.watchcat.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkGlassColorScheme = darkColorScheme(
    primary = Color(0xFF22D3EE),
    onPrimary = OnAccent,
    primaryContainer = Color(0xFF123138),
    onPrimaryContainer = Color(0xFFB8F4FF),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF160C2B),
    secondaryContainer = Color(0xFF2C2350),
    onSecondaryContainer = Color(0xFFE6DCFF),
    tertiary = Color(0xFF60A5FA),
    onTertiary = Color(0xFF001A33),
    background = Color(0xFF040405),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF0A0A0C),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF121216),
    onSurfaceVariant = Color(0xFFA1A1AA),
    surfaceContainer = Color(0xFF0C0C0F),
    surfaceContainerHigh = Color(0xFF131317),
    surfaceContainerHighest = Color(0xFF1A1A1F),
    outline = Color(0xFF33333A),
    outlineVariant = Color(0xFF1F1F24),
    error = Color(0xFFFB7185),
    onError = Color(0xFF2A0710),
    errorContainer = Color(0xFF4A1220),
    onErrorContainer = Color(0xFFFFD9DF),
    inverseSurface = Color(0xFFF5F5F7),
    inverseOnSurface = Color(0xFF111114),
    scrim = Color(0xCC000000),
)

private val LightGlassColorScheme = lightColorScheme(
    primary = Color(0xFF0E7490),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF4FA),
    onPrimaryContainer = Color(0xFF053742),
    secondary = Color(0xFF6D28D9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = Color(0xFF1D4ED8),
    onTertiary = Color.White,
    background = Color(0xFFF7F7F8),
    onBackground = Color(0xFF111114),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111114),
    surfaceVariant = Color(0xFFEFEFF2),
    onSurfaceVariant = Color(0xFF5B5B66),
    surfaceContainer = Color(0xFFF2F2F4),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFFFFFFF),
    outline = Color(0xFFC9C9D1),
    outlineVariant = Color(0xFFE3E3E8),
    error = Color(0xFFBE123C),
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E8),
    onErrorContainer = Color(0xFF4C0519),
    inverseSurface = Color(0xFF111114),
    inverseOnSurface = Color(0xFFF7F7F8),
    scrim = Color(0x99000000),
)

private val GlassShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun WatchCatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    SideEffect { AppThemeState.isDark = darkTheme }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkGlassColorScheme else LightGlassColorScheme,
        typography = AppTypography,
        shapes = GlassShapes,
        content = content,
    )
}
