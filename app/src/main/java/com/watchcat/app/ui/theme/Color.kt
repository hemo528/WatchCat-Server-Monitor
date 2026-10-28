package com.watchcat.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * 当前生效的明暗主题。UI 颜色通过下面的 getter 读取，
 * 切换主题时 Compose 会因 Snapshot 状态变化自动重组。
 */
object AppThemeState {
    var isDark by mutableStateOf(true)
}

val Ink: Color
    get() = if (AppThemeState.isDark) Color(0xFF040405) else Color(0xFFF7F7F8)
val InkElevated: Color
    get() = if (AppThemeState.isDark) Color(0xFF0A0A0C) else Color(0xFFFFFFFF)
val InkSurface: Color
    get() = if (AppThemeState.isDark) Color(0xFF101013) else Color(0xFFF1F1F3)
val InkSurfaceHigh: Color
    get() = if (AppThemeState.isDark) Color(0xFF16161A) else Color(0xFFE9E9ED)
val TextPrimary: Color
    get() = if (AppThemeState.isDark) Color(0xFFF5F5F7) else Color(0xFF111114)
val TextSecondary: Color
    get() = if (AppThemeState.isDark) Color(0xFFA1A1AA) else Color(0xFF5B5B66)
val TextTertiary: Color
    get() = if (AppThemeState.isDark) Color(0xFF6B6B75) else Color(0xFF8A8A94)
val Hairline: Color
    get() = if (AppThemeState.isDark) Color(0xFF33333A) else Color(0xFFD4D4DA)

/** 玻璃卡片、浅色背景上的分隔线/轨道：深色模式为白，浅色模式为黑。 */
val GlassLine: Color
    get() = if (AppThemeState.isDark) Color.White else Color.Black

/** 强调色按钮/头像上的文字颜色。 */
val OnAccent: Color
    get() = if (AppThemeState.isDark) Color(0xFF041015) else Color.White

/** 终端/半透明浮层的底色。 */
val PageBackground: Color
    get() = if (AppThemeState.isDark) Color(0xFF040405) else Color(0xFFF7F7F8)

val OverlayScrim: Color
    get() = if (AppThemeState.isDark) Color.Black else Color.White

val AccentCyan: Color
    get() = if (AppThemeState.isDark) Color(0xFF22D3EE) else Color(0xFF0E7490)
val AccentViolet: Color
    get() = if (AppThemeState.isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9)
val AccentBlue: Color
    get() = if (AppThemeState.isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
val AccentEmerald: Color
    get() = if (AppThemeState.isDark) Color(0xFF34D399) else Color(0xFF047857)
val AccentAmber: Color
    get() = if (AppThemeState.isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
val AccentRose: Color
    get() = if (AppThemeState.isDark) Color(0xFFFB7185) else Color(0xFFBE123C)

val AccentPalette: List<Color>
    get() = listOf(
        AccentCyan,
        AccentViolet,
        AccentBlue,
        AccentEmerald,
        AccentAmber,
        AccentRose,
    )

fun accentAt(index: Int): Color {
    val palette = AccentPalette
    return palette[((index % palette.size) + palette.size) % palette.size]
}

fun usageColor(percent: Float, base: Color): Color = when {
    percent >= 90f -> AccentRose
    percent >= 75f -> AccentAmber
    else -> base
}
